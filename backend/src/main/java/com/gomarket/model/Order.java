package com.gomarket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String status = "PENDING"; // PENDING, ACCEPTED, SHOPPING, DELIVERING, COMPLETED

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    private Double latitude;
    private Double longitude;

    @Column(name = "shopper_name")
    private String shopperName;

    // Plan 1: Thanh toán
    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "COD"; // COD | WALLET | QR

    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PENDING"; // PENDING | PAID | FAILED

    @Column
    private String notes;

    // Plan 2: Vị trí real-time
    @Column(name = "shopper_lat")
    private Double shopperLat;

    @Column(name = "shopper_lng")
    private Double shopperLng;

    @Column(name = "shopper_phone", length = 20)
    private String shopperPhone;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getShopperName() { return shopperName; }
    public void setShopperName(String shopperName) { this.shopperName = shopperName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getShopperLat() { return shopperLat; }
    public void setShopperLat(Double shopperLat) { this.shopperLat = shopperLat; }

    public Double getShopperLng() { return shopperLng; }
    public void setShopperLng(Double shopperLng) { this.shopperLng = shopperLng; }

    public String getShopperPhone() { return shopperPhone; }
    public void setShopperPhone(String shopperPhone) { this.shopperPhone = shopperPhone; }

    public LocalDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatusText() {
        return switch (status) {
            case "PENDING" -> "Đang chờ xác nhận";
            case "ACCEPTED" -> "Đã nhận đơn";
            case "SHOPPING" -> "Đang đi chợ";
            case "DELIVERING" -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            default -> status;
        };
    }
}
