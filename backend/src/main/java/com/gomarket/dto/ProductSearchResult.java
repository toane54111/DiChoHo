package com.gomarket.dto;

/**
 * DTO cho Hybrid Search: kết hợp text search + vector search
 * - matchType = "exact" → tìm thấy bằng LIKE (score = 1.0)
 * - matchType = "semantic" → tìm thấy bằng RAG/vector (score = similarity)
 */
public class ProductSearchResult {
    private Long id;
    private String name;
    private Double price;
    private Double originalPrice;
    private String unit;
    private String category;
    private String imageUrl;
    private String description;
    private Double score;
    private String matchType; // "exact" or "semantic"
    private Integer shopId;
    private String shopName;

    public ProductSearchResult() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public Integer getShopId() { return shopId; }
    public void setShopId(Integer shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
}
