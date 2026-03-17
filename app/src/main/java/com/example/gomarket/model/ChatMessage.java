package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("id")
    private long id;

    @SerializedName("requestId")
    private long requestId;

    @SerializedName("senderId")
    private long senderId;

    @SerializedName("receiverId")
    private long receiverId;

    @SerializedName("message")
    private String message;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("senderName")
    private String senderName;

    public long getId() { return id; }
    public long getRequestId() { return requestId; }
    public long getSenderId() { return senderId; }
    public long getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public String getSenderName() { return senderName; }
}
