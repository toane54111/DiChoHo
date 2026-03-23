package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class CookbookComment {
    @SerializedName("id")
    private long id;

    @SerializedName("recipeId")
    private long recipeId;

    @SerializedName("userId")
    private long userId;

    @SerializedName("content")
    private String content;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("authorAvatar")
    private String authorAvatar;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getRecipeId() { return recipeId; }
    public long getUserId() { return userId; }
    public String getContent() { return content; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public String getCreatedAt() { return createdAt; }
}
