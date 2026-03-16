package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShoppingRequest {
    @SerializedName("id")
    private long id;

    @SerializedName("userId")
    private long userId;

    @SerializedName("shopperId")
    private Long shopperId;

    @SerializedName("status")
    private String status;

    @SerializedName("deliveryAddress")
    private String deliveryAddress;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("budget")
    private Double budget;

    @SerializedName("notes")
    private String notes;

    @SerializedName("paymentMethod")
    private String paymentMethod;

    @SerializedName("paymentStatus")
    private String paymentStatus;

    @SerializedName("totalActualCost")
    private Double totalActualCost;

    @SerializedName("shopperLat")
    private Double shopperLat;

    @SerializedName("shopperLng")
    private Double shopperLng;

    @SerializedName("items")
    private List<ShoppingRequestItem> items;

    @SerializedName("userName")
    private String userName;

    @SerializedName("userPhone")
    private String userPhone;

    @SerializedName("shopperName")
    private String shopperName;

    @SerializedName("shopperPhone")
    private String shopperPhone;

    @SerializedName("statusText")
    private String statusText;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public Long getShopperId() { return shopperId; }
    public String getStatus() { return status; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getBudget() { return budget; }
    public String getNotes() { return notes; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public Double getTotalActualCost() { return totalActualCost; }
    public Double getShopperLat() { return shopperLat; }
    public Double getShopperLng() { return shopperLng; }
    public List<ShoppingRequestItem> getItems() { return items; }
    public String getUserName() { return userName; }
    public String getUserPhone() { return userPhone; }
    public String getShopperName() { return shopperName; }
    public String getShopperPhone() { return shopperPhone; }
    public String getStatusText() { return statusText; }
    public String getCreatedAt() { return createdAt; }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
