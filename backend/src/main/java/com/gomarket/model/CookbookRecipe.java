package com.gomarket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cookbook_recipes")
public class CookbookRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // null = system recipe

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ingredients_json", columnDefinition = "TEXT")
    private String ingredientsJson; // JSON: [{"name":"...", "quantity":"...", "estimated_price":...}]

    @Column(name = "steps_json", columnDefinition = "TEXT")
    private String stepsJson; // JSON: ["Bước 1...", "Bước 2..."]

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "is_system_recipe")
    private Boolean isSystemRecipe = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Transient fields for response
    @Transient
    private String authorName;
    @Transient
    private Integer likeCount;
    @Transient
    private Integer commentCount;
    @Transient
    private Boolean isLikedByUser;

    public CookbookRecipe() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Boolean getIsSystemRecipe() { return isSystemRecipe; }
    public void setIsSystemRecipe(Boolean isSystemRecipe) { this.isSystemRecipe = isSystemRecipe; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public Boolean getIsLikedByUser() { return isLikedByUser; }
    public void setIsLikedByUser(Boolean isLikedByUser) { this.isLikedByUser = isLikedByUser; }
}
