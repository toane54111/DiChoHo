package com.gomarket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(nullable = false, length = 20)
    private String type; // TOP_UP | PAYMENT | REFUND

    // Dương = nạp/hoàn, Âm = chi
    @Column(nullable = false)
    private Long amount;

    @Column
    private String description;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {}

    public WalletTransaction(Long walletId, String type, Long amount, String description, Long orderId) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
