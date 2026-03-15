package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Order {
    @SerializedName("id")
    private int id;

    @SerializedName("userId")
    private int userId;

    @SerializedName("status")
    private String status; // PENDING, ACCEPTED, SHOPPING, DELIVERING, COMPLETED

    @SerializedName("totalPrice")
    private double totalPrice;

    @SerializedName("deliveryAddress")
    private String deliveryAddress;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("items")
    private List<OrderItem> items;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("shopperName")
    private String shopperName;

    public Order() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getShopperName() { return shopperName; }
    public void setShopperName(String shopperName) { this.shopperName = shopperName; }

    public String getFormattedTotal() {
        return String.format("%,.0fđ", totalPrice);
    }

    public String getStatusText() {
        switch (status) {
            case "PENDING": return "Chờ xác nhận";
            case "ACCEPTED": return "Đã nhận đơn";
            case "SHOPPING": return "Đang đi chợ";
            case "DELIVERING": return "Đang giao hàng";
            case "COMPLETED": return "Hoàn thành";
            default: return status;
        }
    }
}
