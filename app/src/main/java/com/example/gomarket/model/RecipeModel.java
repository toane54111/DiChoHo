package com.example.gomarket.model;

import java.io.Serializable;
import java.util.List;

public class RecipeModel implements Serializable {
    private String recipeId;
    private String name;
    private String imageUrl;
    private List<String> ingredients;
    private int cookTime; // in minutes

    public RecipeModel() {
    }

    public RecipeModel(String recipeId, String name, String imageUrl, List<String> ingredients, int cookTime) {
        this.recipeId = recipeId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
        this.cookTime = cookTime;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public int getCookTime() {
        return cookTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }
}
