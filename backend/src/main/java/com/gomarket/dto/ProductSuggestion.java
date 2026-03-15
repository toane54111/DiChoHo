package com.gomarket.dto;

/**
 * DTO nhẹ cho Autocomplete - chỉ chứa thông tin tối thiểu
 * Serve từ RAM cache, không query DB → response < 1ms
 */
public class ProductSuggestion {
    private Long id;
    private String name;
    private String imageUrl;
    private Double price;
    private String category;

    public ProductSuggestion() {}

    public ProductSuggestion(Long id, String name, String imageUrl, Double price, String category) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
