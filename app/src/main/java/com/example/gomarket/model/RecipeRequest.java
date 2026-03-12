package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecipeRequest {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("excludeDishes")
    private List<String> excludeDishes;

    public RecipeRequest(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public RecipeRequest(double latitude, double longitude, List<String> excludeDishes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.excludeDishes = excludeDishes;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<String> getExcludeDishes() { return excludeDishes; }
    public void setExcludeDishes(List<String> excludeDishes) { this.excludeDishes = excludeDishes; }
}
