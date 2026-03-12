package com.example.gomarket;

public class ChatMessage {

    private String message;
    private boolean isMe;
    private String time;

    public ChatMessage(String message, boolean isMe, String time) {
        this.message = message;
        this.isMe = isMe;
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public boolean isMe() {
        return isMe;
    }

    public String getTime() {
        return time;
    }
}
