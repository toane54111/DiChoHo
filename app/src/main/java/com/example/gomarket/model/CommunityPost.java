package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommunityPost {
    @SerializedName("id")
    private long id;

    @SerializedName("userId")
    private long userId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("category")
    private String category;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("locationName")
    private String locationName;

    @SerializedName("region")
    private String region;

    @SerializedName("province")
    private String province;

    @SerializedName("images")
    private List<PostImage> images;

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("authorAvatar")
    private String authorAvatar;

    @SerializedName("authorPhone")
    private String authorPhone;

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("commentCount")
    private int commentCount;

    @SerializedName("isLikedByUser")
    private Boolean isLikedByUser;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getLocationName() { return locationName; }
    public String getProvince() { return province; }
    public String getRegion() { return region; }
    public List<PostImage> getImages() { return images; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public String getAuthorPhone() { return authorPhone; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public Boolean getIsLikedByUser() { return isLikedByUser; }
    public String getCreatedAt() { return createdAt; }

    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setIsLikedByUser(Boolean isLikedByUser) { this.isLikedByUser = isLikedByUser; }

    public String getCategoryDisplay() {
        if (category == null) return "Khác";
        switch (category) {
            case "nong_san": return "Nông sản";
            case "dac_san": return "Đặc sản";
            case "rao_vat": return "Rao vặt";
            case "gom_chung": return "Gom chung";
            default: return "Khác";
        }
    }

    public static class PostImage {
        @SerializedName("id")
        private long id;

        @SerializedName("imageUrl")
        private String imageUrl;

        @SerializedName("displayOrder")
        private int displayOrder;

        public long getId() { return id; }
        public String getImageUrl() { return imageUrl; }
        public int getDisplayOrder() { return displayOrder; }
    }
}
