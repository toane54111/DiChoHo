package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class WalletTransaction {
    @SerializedName("id")
    private long id;

    @SerializedName("walletId")
    private long walletId;

    @SerializedName("type")
    private String type; // TOP_UP, PAYMENT, REFUND

    @SerializedName("amount")
    private long amount;

    @SerializedName("description")
    private String description;

    @SerializedName("orderId")
    private Long orderId;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getWalletId() { return walletId; }
    public String getType() { return type; }
    public long getAmount() { return amount; }
    public String getDescription() { return description; }
    public Long getOrderId() { return orderId; }
    public String getCreatedAt() { return createdAt; }

    public boolean isPositive() {
        return amount >= 0;
    }
}
