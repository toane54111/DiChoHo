package com.example.gomarket.network;

import com.example.gomarket.model.LocationResponse;
import com.example.gomarket.model.LoginRequest;
import com.example.gomarket.model.Order;
import com.example.gomarket.model.OrderRequest;
import com.example.gomarket.model.Product;
import com.example.gomarket.model.RecipeRequest;
import com.example.gomarket.model.RecipeResponse;
import com.example.gomarket.model.RegisterRequest;
import com.example.gomarket.model.User;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.model.WalletTransaction;
import com.example.gomarket.model.WeatherData;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Auth
    @POST("auth/login")
    Call<User> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<User> register(@Body RegisterRequest request);

    @GET("auth/profile/{userId}")
    Call<User> getProfile(@Path("userId") long userId);

    // Recipe - AI Chef
    @GET("recipe/weather")
    Call<WeatherData> getWeather(@Query("latitude") double lat, @Query("longitude") double lng);

    @POST("recipe/suggest")
    Call<RecipeResponse> suggestRecipe(@Body RecipeRequest request);

    // Products
    @GET("products/search")
    Call<List<Product>> searchProducts(@Query("q") String query);

    @GET("products/hybrid-search")
    Call<List<Product>> hybridSearch(@Query("q") String query);

    @GET("products/autocomplete")
    Call<List<Product>> autocomplete(@Query("q") String query);

    @GET("products/category/{category}")
    Call<List<Product>> getByCategory(@Path("category") String category);

    @GET("products/by-category")
    Call<List<Product>> getByCategoryQuery(@Query("name") String category);

    // Orders
    @POST("orders")
    Call<Order> createOrder(@Body OrderRequest request);

    @GET("orders/{id}")
    Call<Order> getOrder(@Path("id") long orderId);

    @PUT("orders/{id}/status")
    Call<Order> updateOrderStatus(@Path("id") long orderId, @Body Map<String, String> body);

    @GET("orders/user/{userId}")
    Call<List<Order>> getUserOrders(@Path("userId") long userId);

    // Plan 2: Realtime Location
    /** Shipper → Backend: cập nhật tọa độ. Chỉ cần 200 OK, không cần deserialize body */
    @PUT("orders/{id}/location")
    Call<Void> updateOrderLocation(@Path("id") long orderId, @Body Map<String, Double> body);

    /** Buyer ← Backend: lấy vị trí shopper hiện tại */
    @GET("orders/{id}/location")
    Call<LocationResponse> getOrderLocation(@Path("id") long orderId);

    // Plan 1: Wallet
    @GET("wallet/{userId}")
    Call<Wallet> getWalletBalance(@Path("userId") long userId);

    @POST("wallet/{userId}/topup")
    Call<Wallet> topUpWallet(@Path("userId") long userId, @Body Map<String, Long> body);

    @GET("wallet/{userId}/transactions")
    Call<List<WalletTransaction>> getWalletTransactions(@Path("userId") long userId);

    @POST("wallet/qr/generate")
    Call<Map<String, String>> generateQrPayload(@Body Map<String, Long> body);

    @POST("wallet/qr/process")
    Call<Wallet> processQrPayload(@Body Map<String, String> body);
}
