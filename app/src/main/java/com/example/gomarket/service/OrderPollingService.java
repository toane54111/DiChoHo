package com.example.gomarket.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.gomarket.R;
import com.example.gomarket.model.Order;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderPollingService extends Service {

    private static final String TAG = "OrderPollingService";
    private static final String CHANNEL_ID = "order_channel";
    private static final int NOTIFICATION_ID = 1002;
    private static final long POLL_INTERVAL = 15000; // 15 giây

    public static final String ACTION_ORDER_STATUS_CHANGED = "com.gomarket.ORDER_STATUS_CHANGED";
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_ORDER_STATUS = "order_status";
    public static final String EXTRA_ORDER_STATUS_TEXT = "order_status_text";

    private Handler handler;
    private Runnable pollingRunnable;
    private int currentOrderId = -1;
    private String lastKnownStatus = "";
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OrderPollingService created");
        handler = new Handler(Looper.getMainLooper());
        apiService = ApiClient.getApiService(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_ORDER_ID)) {
            currentOrderId = intent.getIntExtra(EXTRA_ORDER_ID, -1);
            Log.d(TAG, "Polling order: " + currentOrderId);
        }

        createNotificationChannel();
        Notification notification = buildNotification("Đang theo dõi đơn hàng #" + currentOrderId);
        startForeground(NOTIFICATION_ID, notification);

        startPolling();

        return START_STICKY;
    }

    private void startPolling() {
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentOrderId > 0) {
                    checkOrderStatus();
                }
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };

        handler.post(pollingRunnable);
    }

    private void checkOrderStatus() {
        apiService.getOrder(currentOrderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    String newStatus = order.getStatus();

                    // Nếu trạng thái thay đổi → gửi broadcast
                    if (!newStatus.equals(lastKnownStatus)) {
                        lastKnownStatus = newStatus;

                        Intent broadcastIntent = new Intent(ACTION_ORDER_STATUS_CHANGED);
                        broadcastIntent.putExtra(EXTRA_ORDER_ID, currentOrderId);
                        broadcastIntent.putExtra(EXTRA_ORDER_STATUS, newStatus);
                        broadcastIntent.putExtra(EXTRA_ORDER_STATUS_TEXT, order.getStatusText());
                        sendBroadcast(broadcastIntent);

                        // Cập nhật notification
                        updateNotification("Đơn #" + currentOrderId + ": " + order.getStatusText());

                        // Nếu hoàn thành → hiện notification + dừng service
                        if ("COMPLETED".equals(newStatus)) {
                            showCompletionNotification();
                            stopSelf();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Log.e(TAG, "Polling failed: " + t.getMessage());
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Theo dõi đơn hàng",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Thông báo trạng thái đơn hàng");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đi Chợ Hộ")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private void showCompletionNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Đơn hàng hoàn thành!")
                    .setContentText("Đơn hàng #" + currentOrderId + " đã được giao thành công")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build();
            manager.notify(NOTIFICATION_ID + 1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
        Log.d(TAG, "OrderPollingService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
