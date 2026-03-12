package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Route {
    @SerializedName("shops")
    private List<Shop> orderedShops;

    @SerializedName("total_distance")
    private double totalDistance;

    @SerializedName("estimated_time")
    private int estimatedTimeMinutes;

    public Route() {}

    public List<Shop> getOrderedShops() { return orderedShops; }
    public void setOrderedShops(List<Shop> orderedShops) { this.orderedShops = orderedShops; }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }

    public int getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
    public void setEstimatedTimeMinutes(int estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }

    public String getFormattedDistance() {
        if (totalDistance < 1.0) {
            return String.format("%.0fm", totalDistance * 1000);
        }
        return String.format("%.1fkm", totalDistance);
    }

    public String getFormattedTime() {
        return String.format("~%d phút", estimatedTimeMinutes);
    }
}
