package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrderRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("delivery_address")
    private String deliveryAddress;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("items")
    private List<OrderItemRequest> items;

    @SerializedName("paymentMethod")
    private String paymentMethod = "COD";

    public OrderRequest(int userId, String deliveryAddress, double latitude, double longitude,
                        List<OrderItemRequest> items, String paymentMethod) {
        this.userId = userId;
        this.deliveryAddress = deliveryAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.items = items;
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentMethod() { return paymentMethod; }

    public static class OrderItemRequest {
        @SerializedName("product_id")
        private int productId;

        @SerializedName("quantity")
        private int quantity;

        public OrderItemRequest(int productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
