package com.example.gomarket.network;

import android.content.Context;

import com.example.gomarket.util.SessionManager;

import java.io.IOException;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Emulator: 10.0.2.2 maps to host machine's localhost
    // Thay bằng IP server thật khi deploy
    private static final String SERVER_ROOT = "http://10.0.2.2:8080";
    //private static final String SERVER_ROOT = "http://192.168.100.182:8080";
    private static final String BASE_URL = SERVER_ROOT + "/api/";

    /**
     * Chuyển đường dẫn ảnh relative (/product-images/xxx.jpg) → URL đầy đủ.
     * Nếu đã là URL đầy đủ (http/https) thì giữ nguyên.
     */
    public static String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        if (imageUrl.startsWith("http")) return imageUrl; // URL CDN cũ hoặc URL đầy đủ
        return SERVER_ROOT + imageUrl; // /product-images/xxx.jpg → http://10.0.2.2:8080/product-images/xxx.jpg
    }

    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            SessionManager sessionManager = new SessionManager(context);

            // Logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            // Auth interceptor
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
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
                            builder.header("Content-Type", "application/json");

                            return chain.proceed(builder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }

    public static void setBaseUrl(String url) {
        retrofit = null;
        apiService = null;
    }
}
