package com.example.gomarket.model;

import java.io.Serializable;

public class ShopperModel implements Serializable {
    private String shopperId;
    private String name;
    private String avatarUrl;
    private float rating;
    private int completedOrders;
    private double distance; // in km
    private boolean isOnline;

    public ShopperModel() {
    }

    public ShopperModel(String shopperId, String name, String avatarUrl, float rating, int completedOrders, double distance, boolean isOnline) {
        this.shopperId = shopperId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.rating = rating;
        this.completedOrders = completedOrders;
        this.distance = distance;
        this.isOnline = isOnline;
    }

    public String getShopperId() {
        return shopperId;
    }

    public void setShopperId(String shopperId) {
        this.shopperId = shopperId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
