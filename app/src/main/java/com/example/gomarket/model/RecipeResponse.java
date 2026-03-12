package com.example.gomarket.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecipeResponse {
    @SerializedName("weather")
    private WeatherData weather;

    @SerializedName("recipe")
    private Recipe recipe;

    @SerializedName("products")
    private List<Product> products;

    @SerializedName("route")
    private Route route;

    @SerializedName("alternative_recipes")
    private List<Recipe> alternativeRecipes;

    public RecipeResponse() {}

    public WeatherData getWeather() { return weather; }
    public void setWeather(WeatherData weather) { this.weather = weather; }

    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public List<Recipe> getAlternativeRecipes() { return alternativeRecipes; }
    public void setAlternativeRecipes(List<Recipe> alternativeRecipes) { this.alternativeRecipes = alternativeRecipes; }
}
