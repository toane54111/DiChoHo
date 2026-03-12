package com.gomarket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.pgvector.PGvector;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double price;

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(length = 50)
    private String unit;

    @Column(length = 100)
    private String category;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    /**
     * Vector embedding 1024 chiều từ Ollama bge-m3 (local, đa ngôn ngữ)
     * Dùng cho RAG: tìm sản phẩm bằng cosine similarity
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    @Convert(converter = com.gomarket.config.VectorTypeConverter.class)
    private float[] embedding;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Product() {}

    public Product(String name, Double price, String unit, String category, String imageUrl, String description, Shop shop) {
        this.name = name;
        this.price = price;
        this.originalPrice = price;
        this.unit = unit;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.shop = shop;
        this.createdAt = LocalDateTime.now();
    }

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

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
}
