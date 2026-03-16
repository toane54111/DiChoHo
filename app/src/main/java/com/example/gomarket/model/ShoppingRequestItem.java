package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class ShoppingRequestItem {
    @SerializedName("id")
    private long id;

    @SerializedName("itemText")
    private String itemText;

    @SerializedName("quantityNote")
    private String quantityNote;

    @SerializedName("isPurchased")
    private boolean isPurchased;

    @SerializedName("actualPrice")
    private Double actualPrice;

    @SerializedName("note")
    private String note;

    public long getId() { return id; }
    public String getItemText() { return itemText; }
    public String getQuantityNote() { return quantityNote; }
    public boolean isPurchased() { return isPurchased; }
    public Double getActualPrice() { return actualPrice; }
    public String getNote() { return note; }

    public void setIsPurchased(boolean isPurchased) { this.isPurchased = isPurchased; }
    public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }
    public void setNote(String note) { this.note = note; }
}
