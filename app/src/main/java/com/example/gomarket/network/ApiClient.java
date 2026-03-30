package com.example.gomarket.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.gomarket.util.SessionManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final int SERVER_PORT = 8080;

    private static String serverRoot = null;
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static boolean initialized = false;

    /**
     * Khởi tạo async — gọi trong LoginActivity.onCreate() trước mọi API call.
     * Tự nhận diện emulator/máy thật, tìm IP backend trên mạng.
     */
    public static void initAsync(Context context, Runnable onReady) {
        if (initialized && serverRoot != null) {
            onReady.run();
            return;
        }

        new Thread(() -> {
            detectServerRoot(context);
            initialized = true;
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Server: " + serverRoot, Toast.LENGTH_LONG).show();
                Log.i(TAG, "Server detected: " + serverRoot);
                onReady.run();
            });
        }).start();
    }

    private static void detectServerRoot(Context context) {
        if (serverRoot != null) return;

        // 1) Emulator → 10.0.2.2
        if (isEmulator()) {
            serverRoot = "http://10.0.2.2:" + SERVER_PORT;
            Log.d(TAG, "Emulator → " + serverRoot);
            return;
        }

        // 2) Máy thật: lấy IP qua NetworkInterface (tin cậy, không cần permission)
        String deviceIp = getDeviceWifiIp();
        Log.d(TAG, "Device WiFi IP: " + deviceIp);

        if (deviceIp != null) {
            // Thử scan subnet tìm backend
            String foundIp = scanSubnetForServer(deviceIp);
            if (foundIp != null) {
                serverRoot = "http://" + foundIp + ":" + SERVER_PORT;
                Log.d(TAG, "Found backend: " + serverRoot);
                return;
            }
        }

        // 3) Fallback: thử lấy IP từ WifiManager (DhcpInfo gateway)
        String gatewayIp = getGatewayIp(context);
        if (gatewayIp != null && deviceIp != null) {
            // Backend thường cùng subnet với gateway
            // Thử gateway subnet + scan thêm
            Log.d(TAG, "Gateway: " + gatewayIp + ", trying nearby IPs...");
        }

        // 4) Fallback cuối: dùng 10.0.2.2
        if (serverRoot == null) {
            serverRoot = "http://10.0.2.2:" + SERVER_PORT;
            Log.w(TAG, "Fallback → " + serverRoot);
        }
    }

    /**
     * Nhận diện emulator
     */
    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("google/sdk_gphone")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("sdk_gphone")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    /**
     * Lấy IP WiFi qua NetworkInterface — không cần permission, hoạt động trên mọi API level.
     */
    private static String getDeviceWifiIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;

                // Ưu tiên wlan0 (WiFi) hoặc interface không phải rmnet (cellular)
                String name = ni.getName().toLowerCase();
                if (name.contains("rmnet") || name.contains("dummy") || name.contains("lo")) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    // Chỉ lấy IPv4
                    if (ip != null && ip.indexOf(':') < 0 && !ip.startsWith("127.")) {
                        Log.d(TAG, "Found IP on " + ni.getName() + ": " + ip);
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "NetworkInterface error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy gateway IP từ WifiManager DhcpInfo
     */
    private static String getGatewayIp(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            int gw = wm.getDhcpInfo().gateway;
            if (gw == 0) return null;
            return String.format("%d.%d.%d.%d",
                    (gw & 0xff), (gw >> 8 & 0xff), (gw >> 16 & 0xff), (gw >> 24 & 0xff));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Scan subnet tìm máy đang chạy backend port 8080.
     * 64 threads song song, timeout 1.5s mỗi IP, chờ tối đa 5s.
     */
    private static String scanSubnetForServer(String deviceIp) {
        String subnet = deviceIp.substring(0, deviceIp.lastIndexOf('.') + 1);
        AtomicReference<String> foundIp = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(64);

        Log.d(TAG, "Scanning subnet " + subnet + "0/24 for port " + SERVER_PORT);

        for (int i = 1; i <= 254; i++) {
            String ip = subnet + i;
            if (ip.equals(deviceIp)) continue;

            executor.submit(() -> {
                if (foundIp.get() != null) return;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ip, SERVER_PORT), 1500);
                    Log.d(TAG, "Port " + SERVER_PORT + " open on " + ip);
                    if (foundIp.compareAndSet(null, ip)) {
                        latch.countDown();
                    }
                } catch (Exception ignored) {}
            });
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        executor.shutdownNow();
        String result = foundIp.get();
        Log.d(TAG, "Scan result: " + (result != null ? result : "not found"));
        return result;
    }

    /**
     * URL đầy đủ cho ảnh
     */
    public static String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl;
        return (serverRoot != null ? serverRoot : "http://10.0.2.2:" + SERVER_PORT) + imageUrl;
    }

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            // Nếu chưa init async, detect sync (fallback)
            if (serverRoot == null) {
                detectServerRoot(context);
            }

            String baseUrl = serverRoot + "/api/";
            SessionManager sessionManager = new SessionManager(context);

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = sessionManager.getToken();

                            Request.Builder builder = original.newBuilder();
                            if (token != null && !token.isEmpty()) {
                                builder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
            Log.i(TAG, "ApiService → " + baseUrl);
        }
        return apiService;
    }

    public static String getServerRoot() {
        return serverRoot;
    }

    public static void resetConnection() {
        serverRoot = null;
        retrofit = null;
        apiService = null;
        initialized = false;
    }
}
