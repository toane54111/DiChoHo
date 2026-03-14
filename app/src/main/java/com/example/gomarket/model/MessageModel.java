package com.example.gomarket.model;

public class MessageModel {
    private String messageId;
    private String chatId;
    private String senderId;
    private String text;
    private long timestamp;

    public MessageModel() {
    }

    public MessageModel(String messageId, String chatId, String senderId, String text, long timestamp) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
