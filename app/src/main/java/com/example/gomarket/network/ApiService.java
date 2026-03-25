package com.example.gomarket.network;

import com.example.gomarket.model.ChatMessage;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.model.LocationResponse;
import com.example.gomarket.model.LocalGuideResponse;
import com.example.gomarket.model.LoginRequest;
import com.example.gomarket.model.Order;
import com.example.gomarket.model.OrderRequest;
import com.example.gomarket.model.PostComment;
import com.example.gomarket.model.Product;
import com.example.gomarket.model.RecipeRequest;
import com.example.gomarket.model.RecipeResponse;
import com.example.gomarket.model.RegisterRequest;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.ShopperReview;
import com.example.gomarket.model.User;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.model.WalletTransaction;
import com.example.gomarket.model.WeatherData;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ═══ Auth ═══
    @POST("auth/login")
    Call<User> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<User> register(@Body RegisterRequest request);

    @GET("auth/profile/{userId}")
    Call<User> getProfile(@Path("userId") long userId);

    @PUT("auth/{userId}/online-status")
    Call<Map<String, String>> updateOnlineStatus(@Path("userId") long userId, @Body Map<String, Boolean> body);

    @PUT("auth/{userId}/location")
    Call<Map<String, String>> updateUserLocation(@Path("userId") long userId, @Body Map<String, Double> body);

    @GET("auth/shoppers/nearby")
    Call<List<User>> getNearbyShoppers(@Query("lat") double lat, @Query("lng") double lng);

    @PUT("auth/{userId}/profile")
    Call<User> updateProfile(@Path("userId") long userId, @Body Map<String, String> body);

    // ═══ Shopping Requests (Đi chợ hộ) ═══
    @POST("shopping-requests")
    Call<ShoppingRequest> createShoppingRequest(@Body Map<String, Object> body);

    @GET("shopping-requests/{id}")
    Call<ShoppingRequest> getShoppingRequest(@Path("id") long id);

    @GET("shopping-requests/user/{userId}")
    Call<List<ShoppingRequest>> getUserShoppingRequests(@Path("userId") long userId);

    @GET("shopping-requests/shopper/{shopperId}")
    Call<List<ShoppingRequest>> getShopperRequests(@Path("shopperId") long shopperId);

    @GET("shopping-requests/nearby")
    Call<List<ShoppingRequest>> getNearbyRequests(@Query("lat") double lat, @Query("lng") double lng);

    @PUT("shopping-requests/{id}/accept")
    Call<ShoppingRequest> acceptRequest(@Path("id") long id, @Body Map<String, Object> body);

    @PUT("shopping-requests/{id}/status")
    Call<ShoppingRequest> updateRequestStatus(@Path("id") long id, @Body Map<String, String> body);

    @PUT("shopping-requests/{id}/items/{itemId}")
    Call<Map<String, Object>> updateRequestItem(@Path("id") long reqId, @Path("itemId") long itemId, @Body Map<String, Object> body);

    @PUT("shopping-requests/{id}/location")
    Call<Void> updateRequestLocation(@Path("id") long id, @Body Map<String, Double> body);

    @PUT("shopping-requests/{id}/cancel")
    Call<ShoppingRequest> cancelRequest(@Path("id") long id);

    // ═══ Community Posts (Chợ đồng hương) ═══
    @POST("posts")
    Call<CommunityPost> createPost(@Body Map<String, Object> body);

    @GET("posts/feed")
    Call<List<CommunityPost>> getFeed(@Query("lat") Double lat, @Query("lng") Double lng,
                                       @Query("page") int page, @Query("category") String category,
                                       @Query("region") String region, @Query("province") String province);

    @GET("posts/provinces")
    Call<Map<String, List<String>>> getProvinces();

    @GET("posts/search")
    Call<List<CommunityPost>> searchPosts(@Query("q") String query);

    @GET("posts/{id}")
    Call<CommunityPost> getPost(@Path("id") long id, @Query("userId") Long userId);

    @GET("posts/user/{userId}")
    Call<List<CommunityPost>> getUserPosts(@Path("userId") long userId);

    @POST("posts/{id}/like")
    Call<Map<String, Object>> toggleLike(@Path("id") long id, @Query("userId") long userId);

    @POST("posts/{id}/comments")
    Call<PostComment> addComment(@Path("id") long id, @Body Map<String, Object> body);

    @GET("posts/{id}/comments")
    Call<List<PostComment>> getComments(@Path("id") long id);

    @DELETE("posts/{id}")
    Call<Map<String, String>> deletePost(@Path("id") long id, @Query("userId") long userId);

    // ═══ Recipe - AI Chef ═══
    @GET("recipe/weather")
    Call<WeatherData> getWeather(@Query("latitude") double lat, @Query("longitude") double lng);

    @POST("recipe/suggest")
    Call<RecipeResponse> suggestRecipe(@Body RecipeRequest request);

    @POST("recipe/to-shopping-request")
    Call<ShoppingRequest> recipeToShoppingRequest(@Body Map<String, Object> body);

    // ═══ Cookbook (Sổ tay nấu ăn) ═══
    @POST("cookbook")
    Call<com.example.gomarket.model.CookbookRecipe> createCookbookRecipe(@Body Map<String, Object> body);

    @GET("cookbook/suggestions")
    Call<List<com.example.gomarket.model.CookbookRecipe>> getCookbookSuggestions(@Query("page") int page);

    @GET("cookbook/community")
    Call<List<com.example.gomarket.model.CookbookRecipe>> getCookbookCommunity(@Query("page") int page, @Query("userId") Long userId);

    @GET("cookbook/personal/{userId}")
    Call<List<com.example.gomarket.model.CookbookRecipe>> getCookbookPersonal(@Path("userId") long userId);

    @GET("cookbook/{id}")
    Call<com.example.gomarket.model.CookbookRecipe> getCookbookRecipe(@Path("id") long id, @Query("userId") Long userId);

    @POST("cookbook/{id}/like")
    Call<Map<String, Object>> toggleCookbookLike(@Path("id") long id, @Query("userId") long userId);

    @POST("cookbook/{id}/comments")
    Call<com.example.gomarket.model.CookbookComment> addCookbookComment(@Path("id") long id, @Body Map<String, Object> body);

    @GET("cookbook/{id}/comments")
    Call<List<com.example.gomarket.model.CookbookComment>> getCookbookComments(@Path("id") long id);

    @DELETE("cookbook/{id}")
    Call<Map<String, String>> deleteCookbookRecipe(@Path("id") long id, @Query("userId") long userId);

    // ═══ AI Thổ Địa (Local Guide) ═══
    @GET("local-guide/suggestions")
    Call<LocalGuideResponse> getLocalGuideSuggestions(
            @Query("userId") long userId,
            @Query("lat") double lat,
            @Query("lng") double lng);

    // ═══ Products (Từ điển vật giá) ═══
    @GET("products/search")
    Call<List<Product>> searchProducts(@Query("q") String query);

    @GET("products/hybrid-search")
    Call<List<Product>> hybridSearch(@Query("q") String query);

    @GET("products/autocomplete")
    Call<List<Product>> autocomplete(@Query("q") String query);

    @GET("products/by-category")
    Call<List<Product>> getByCategoryQuery(@Query("name") String category);

    // ═══ Orders (legacy) ═══
    @POST("orders")
    Call<Order> createOrder(@Body OrderRequest request);

    @GET("orders/{id}")
    Call<Order> getOrder(@Path("id") long orderId);

    @PUT("orders/{id}/status")
    Call<Order> updateOrderStatus(@Path("id") long orderId, @Body Map<String, String> body);

    @GET("orders/user/{userId}")
    Call<List<Order>> getUserOrders(@Path("userId") long userId);

    @PUT("orders/{id}/location")
    Call<Void> updateOrderLocation(@Path("id") long orderId, @Body Map<String, Double> body);

    @GET("orders/{id}/location")
    Call<LocationResponse> getOrderLocation(@Path("id") long orderId);

    // ═══ Wallet ═══
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

    // ═══ Reviews (Đánh giá shopper) ═══
    @POST("reviews")
    Call<ShopperReview> createReview(@Body Map<String, Object> body);

    @GET("reviews/shopper/{shopperId}/summary")
    Call<Map<String, Object>> getShopperRatingSummary(@Path("shopperId") long shopperId);

    @GET("reviews/request/{requestId}")
    Call<Map<String, Object>> getReviewForRequest(@Path("requestId") long requestId);

    // ═══ Chat (Tin nhắn đơn hàng) ═══
    @POST("chat/send")
    Call<ChatMessage> sendChatMessage(@Body Map<String, Object> body);

    @GET("chat/{requestId}/messages")
    Call<List<ChatMessage>> getChatMessages(@Path("requestId") long requestId, @Query("afterId") Long afterId);

    @GET("chat/conversations/{userId}")
    Call<List<Map<String, Object>>> getConversations(@Path("userId") long userId);

    // ═══ Upload ═══
    @Multipart
    @POST("upload/image")
    Call<Map<String, String>> uploadImage(@Part MultipartBody.Part file);
}
