package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CookbookRecipe {
    @SerializedName("id")
    private long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("ingredientsJson")
    private String ingredientsJson;

    @SerializedName("stepsJson")
    private String stepsJson;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("totalCost")
    private double totalCost;

    @SerializedName("isSystemRecipe")
    private boolean isSystemRecipe;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("commentCount")
    private int commentCount;

    @SerializedName("isLikedByUser")
    private boolean isLikedByUser;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredientsJson() { return ingredientsJson; }
    public void setIngredientsJson(String ingredientsJson) { this.ingredientsJson = ingredientsJson; }

    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public boolean isSystemRecipe() { return isSystemRecipe; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isLikedByUser() { return isLikedByUser; }
    public void setIsLikedByUser(boolean liked) { this.isLikedByUser = liked; }

    public String getCreatedAt() { return createdAt; }

    public String getFormattedTotalCost() {
        return String.format("~%,.0fđ", totalCost);
    }
}
