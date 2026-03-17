package com.gomarket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shopping_requests")
public class ShoppingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shopper_id")
    private Long shopperId;

    @Column(length = 50)
    private String status = "OPEN"; // OPEN, ACCEPTED, SHOPPING, DELIVERING, COMPLETED, CANCELLED

    @Column(name = "delivery_address")
    private String deliveryAddress;

    private Double latitude;
    private Double longitude;

    private Double budget;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "COD";

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PENDING";

    @Column(name = "shopper_fee")
    private Double shopperFee = 0.0;

    @Column(name = "frozen_amount")
    private Double frozenAmount = 0.0;

    @Column(name = "total_actual_cost")
    private Double totalActualCost;

    @Column(name = "shopper_lat")
    private Double shopperLat;

    @Column(name = "shopper_lng")
    private Double shopperLng;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @OneToMany(mappedBy = "shoppingRequest", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ShoppingRequestItem> items;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Transient fields for response
    @Transient
    private String userName;
    @Transient
    private String userPhone;
    @Transient
    private String shopperName;
    @Transient
    private String shopperPhone;

    public ShoppingRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getShopperId() { return shopperId; }
    public void setShopperId(Long shopperId) { this.shopperId = shopperId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Double getShopperFee() { return shopperFee; }
    public void setShopperFee(Double shopperFee) { this.shopperFee = shopperFee; }

    public Double getFrozenAmount() { return frozenAmount; }
    public void setFrozenAmount(Double frozenAmount) { this.frozenAmount = frozenAmount; }

    public Double getTotalActualCost() { return totalActualCost; }
    public void setTotalActualCost(Double totalActualCost) { this.totalActualCost = totalActualCost; }

    public Double getShopperLat() { return shopperLat; }
    public void setShopperLat(Double shopperLat) { this.shopperLat = shopperLat; }

    public Double getShopperLng() { return shopperLng; }
    public void setShopperLng(Double shopperLng) { this.shopperLng = shopperLng; }

    public LocalDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public List<ShoppingRequestItem> getItems() { return items; }
    public void setItems(List<ShoppingRequestItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getShopperName() { return shopperName; }
    public void setShopperName(String shopperName) { this.shopperName = shopperName; }

    public String getShopperPhone() { return shopperPhone; }
    public void setShopperPhone(String shopperPhone) { this.shopperPhone = shopperPhone; }

    public String getStatusText() {
        return switch (status) {
            case "OPEN" -> "Đang tìm shopper";
            case "ACCEPTED" -> "Shopper đã nhận đơn";
            case "SHOPPING" -> "Đang đi chợ";
            case "DELIVERING" -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
