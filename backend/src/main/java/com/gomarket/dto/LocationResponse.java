package com.gomarket.dto;

import java.time.LocalDateTime;

public class LocationResponse {
    private Double shopperLat;
    private Double shopperLng;
    private String shopperName;
    private String shopperPhone;
    private LocalDateTime locationUpdatedAt;

    public LocationResponse() {}

    public LocationResponse(Double shopperLat, Double shopperLng,
                            String shopperName, String shopperPhone,
                            LocalDateTime locationUpdatedAt) {
        this.shopperLat = shopperLat;
        this.shopperLng = shopperLng;
        this.shopperName = shopperName;
        this.shopperPhone = shopperPhone;
        this.locationUpdatedAt = locationUpdatedAt;
    }

    public Double getShopperLat() { return shopperLat; }
    public void setShopperLat(Double shopperLat) { this.shopperLat = shopperLat; }
    public Double getShopperLng() { return shopperLng; }
    public void setShopperLng(Double shopperLng) { this.shopperLng = shopperLng; }
    public String getShopperName() { return shopperName; }
    public void setShopperName(String shopperName) { this.shopperName = shopperName; }
    public String getShopperPhone() { return shopperPhone; }
    public void setShopperPhone(String shopperPhone) { this.shopperPhone = shopperPhone; }
    public LocalDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }
}
