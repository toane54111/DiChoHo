package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Recipe {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("weather_context")
    private String weatherContext;

    @SerializedName("ingredients")
    private List<Ingredient> ingredients;

    @SerializedName("steps")
    private List<String> steps;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("total_cost")
    private double totalCost;

    public Recipe() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWeatherContext() { return weatherContext; }
    public void setWeatherContext(String weatherContext) { this.weatherContext = weatherContext; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getFormattedTotalCost() {
        return String.format("~%,.0fđ", totalCost);
    }

    public static class Ingredient {
        @SerializedName("name")
        private String name;

        @SerializedName("quantity")
        private String quantity;

        @SerializedName("estimated_price")
        private double estimatedPrice;

        @SerializedName("product")
        private Product matchedProduct;

        public Ingredient() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }

        public double getEstimatedPrice() { return estimatedPrice; }
        public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

        public Product getMatchedProduct() { return matchedProduct; }
        public void setMatchedProduct(Product matchedProduct) { this.matchedProduct = matchedProduct; }
    }
}
