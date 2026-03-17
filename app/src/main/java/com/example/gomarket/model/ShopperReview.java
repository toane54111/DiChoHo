package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class ShopperReview {
    @SerializedName("id")
    private long id;

    @SerializedName("requestId")
    private long requestId;

    @SerializedName("buyerId")
    private long buyerId;

    @SerializedName("shopperId")
    private long shopperId;

    @SerializedName("rating")
    private int rating;

    @SerializedName("comment")
    private String comment;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("buyerName")
    private String buyerName;

    public long getId() { return id; }
    public long getRequestId() { return requestId; }
    public long getBuyerId() { return buyerId; }
    public long getShopperId() { return shopperId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public String getBuyerName() { return buyerName; }
}
