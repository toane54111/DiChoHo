package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class Shop {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private String address;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("category")
    private String category;

    public Shop() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
