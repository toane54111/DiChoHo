package com.example.gomarket.model;

import java.io.Serializable;
import java.util.List;

public class OrderModel implements Serializable {
    private String orderId;
    private String userId;
    private String shopperId;
    private String recipeId;
    private List<String> missingIngredients;
    private String status;
    private long timestamp;

    public OrderModel() {
    }

    public OrderModel(String orderId, String userId, String shopperId, String recipeId, List<String> missingIngredients, String status, long timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.shopperId = shopperId;
        this.recipeId = recipeId;
        this.missingIngredients = missingIngredients;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShopperId() {
        return shopperId;
    }

    public void setShopperId(String shopperId) {
        this.shopperId = shopperId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public List<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
