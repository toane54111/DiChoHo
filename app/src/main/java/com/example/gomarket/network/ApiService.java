package com.example.gomarket.network;

import com.example.gomarket.model.LoginRequest;
import com.example.gomarket.model.Order;
import com.example.gomarket.model.OrderRequest;
import com.example.gomarket.model.Product;
import com.example.gomarket.model.RecipeRequest;
import com.example.gomarket.model.RecipeResponse;
import com.example.gomarket.model.RegisterRequest;
import com.example.gomarket.model.User;
import com.example.gomarket.model.WeatherData;

import java.util.List;

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

    // Recipe - AI Chef (Core)
    @GET("recipe/weather")
    Call<WeatherData> getWeather(@Query("latitude") double lat, @Query("longitude") double lng);

    @POST("recipe/suggest")
    Call<RecipeResponse> suggestRecipe(@Body RecipeRequest request);

    // Products
    @GET("products/search")
    Call<List<Product>> searchProducts(@Query("q") String query);

    @GET("products/category/{category}")
    Call<List<Product>> getByCategory(@Path("category") String category);

    // Orders
    @POST("orders")
    Call<Order> createOrder(@Body OrderRequest request);

    @GET("orders/{id}")
    Call<Order> getOrder(@Path("id") int orderId);

    @PUT("orders/{id}/status")
    Call<Order> updateOrderStatus(@Path("id") int orderId, @Body Order order);

    @GET("orders/user/{userId}")
    Call<List<Order>> getUserOrders(@Path("userId") int userId);
}
