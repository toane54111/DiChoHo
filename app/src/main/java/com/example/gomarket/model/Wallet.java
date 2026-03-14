package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class Wallet {
    @SerializedName("id")
    private long id;

    @SerializedName("userId")
    private long userId;

    @SerializedName("balance")
    private long balance; // đồng VNĐ (Long)

    @SerializedName("updatedAt")
    private String updatedAt;

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public long getBalance() { return balance; }
    public String getUpdatedAt() { return updatedAt; }
}
