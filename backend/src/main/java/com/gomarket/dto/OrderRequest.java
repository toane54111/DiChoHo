package com.gomarket.dto;

import java.util.List;

public class OrderRequest {
    private Long user_id;
    private String delivery_address;
    private double latitude;
    private double longitude;
    private List<OrderItemRequest> items;
    private String paymentMethod = "COD"; // COD | WALLET | QR
    private String notes;

    public static class OrderItemRequest {
        private Long product_id;
        private int quantity;

        public Long getProduct_id() { return product_id; }
        public void setProduct_id(Long product_id) { this.product_id = product_id; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public Long getUser_id() { return user_id; }
    public void setUser_id(Long user_id) { this.user_id = user_id; }
    public String getDelivery_address() { return delivery_address; }
    public void setDelivery_address(String delivery_address) { this.delivery_address = delivery_address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
