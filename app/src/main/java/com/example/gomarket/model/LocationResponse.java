package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class LocationResponse {
    @SerializedName("shopperLat")
    private double shopperLat;

    @SerializedName("shopperLng")
    private double shopperLng;

    @SerializedName("shopperName")
    private String shopperName;

    @SerializedName("shopperPhone")
    private String shopperPhone;

    @SerializedName("locationUpdatedAt")
    private String locationUpdatedAt;

    public double getShopperLat() { return shopperLat; }
    public double getShopperLng() { return shopperLng; }
    public String getShopperName() { return shopperName; }
    public String getShopperPhone() { return shopperPhone; }
    public String getLocationUpdatedAt() { return locationUpdatedAt; }
}
