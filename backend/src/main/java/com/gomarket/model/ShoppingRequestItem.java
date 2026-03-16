package com.gomarket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "shopping_request_items")
public class ShoppingRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    @JsonIgnore
    private ShoppingRequest shoppingRequest;

    @Column(name = "item_text", nullable = false)
    private String itemText; // e.g. "2kg thịt heo", "1 bắp cải"

    @Column(name = "quantity_note", length = 100)
    private String quantityNote; // e.g. "khoảng 2kg"

    @Column(name = "is_purchased")
    private Boolean isPurchased = false;

    @Column(name = "actual_price")
    private Double actualPrice;

    @Column(length = 255)
    private String note; // e.g. "hết hàng, thay bằng..."

    public ShoppingRequestItem() {}

    public ShoppingRequestItem(String itemText, String quantityNote) {
        this.itemText = itemText;
        this.quantityNote = quantityNote;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShoppingRequest getShoppingRequest() { return shoppingRequest; }
    public void setShoppingRequest(ShoppingRequest shoppingRequest) { this.shoppingRequest = shoppingRequest; }

    public String getItemText() { return itemText; }
    public void setItemText(String itemText) { this.itemText = itemText; }

    public String getQuantityNote() { return quantityNote; }
    public void setQuantityNote(String quantityNote) { this.quantityNote = quantityNote; }

    public Boolean getIsPurchased() { return isPurchased; }
    public void setIsPurchased(Boolean isPurchased) { this.isPurchased = isPurchased; }

    public Double getActualPrice() { return actualPrice; }
    public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
