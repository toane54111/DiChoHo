package com.gomarket.dto;

import java.util.List;

public class RecipeRequest {
    private double latitude;
    private double longitude;
    private List<String> excludeDishes;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public List<String> getExcludeDishes() { return excludeDishes; }
    public void setExcludeDishes(List<String> excludeDishes) { this.excludeDishes = excludeDishes; }
}
