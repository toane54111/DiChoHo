package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

public class WeatherData {
    @SerializedName("temp")
    private double temp;

    @SerializedName("description")
    private String description;

    @SerializedName("humidity")
    private int humidity;

    @SerializedName("icon")
    private String icon;

    @SerializedName("city")
    private String city;

    public WeatherData() {}

    public double getTemp() { return temp; }
    public void setTemp(double temp) { this.temp = temp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getFormattedTemp() {
        return String.format("%.0f°C", temp);
    }
}
