package com.gomarket.dto;

import java.util.List;

public class RecipeResponse {

    private WeatherInfo weather;
    private RecipeInfo recipe;
    private List<ProductInfo> products;
    private RouteInfo route;
    private List<RecipeInfo> alternative_recipes;

    // Weather
    public static class WeatherInfo {
        private double temp;
        private String description;
        private int humidity;
        private String icon;
        private String city;

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
    }

    // Recipe
    public static class RecipeInfo {
        private String name;
        private String description;
        private String weather_context;
        private List<IngredientInfo> ingredients;
        private List<String> steps;
        private String image_url;
        private double total_cost;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getWeather_context() { return weather_context; }
        public void setWeather_context(String weather_context) { this.weather_context = weather_context; }
        public List<IngredientInfo> getIngredients() { return ingredients; }
        public void setIngredients(List<IngredientInfo> ingredients) { this.ingredients = ingredients; }
        public List<String> getSteps() { return steps; }
        public void setSteps(List<String> steps) { this.steps = steps; }
        public String getImage_url() { return image_url; }
        public void setImage_url(String image_url) { this.image_url = image_url; }
        public double getTotal_cost() { return total_cost; }
        public void setTotal_cost(double total_cost) { this.total_cost = total_cost; }
    }

    // Ingredient
    public static class IngredientInfo {
        private String name;
        private String quantity;
        private ProductInfo product;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }
        public ProductInfo getProduct() { return product; }
        public void setProduct(ProductInfo product) { this.product = product; }
    }

    // Product
    public static class ProductInfo {
        private Long id;
        private String name;
        private double price;
        private double original_price;
        private String unit;
        private String category;
        private String image_url;
        private String description;
        private int shop_id;
        private String shop_name;
        private double similarity;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getOriginal_price() { return original_price; }
        public void setOriginal_price(double original_price) { this.original_price = original_price; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getImage_url() { return image_url; }
        public void setImage_url(String image_url) { this.image_url = image_url; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getShop_id() { return shop_id; }
        public void setShop_id(int shop_id) { this.shop_id = shop_id; }
        public String getShop_name() { return shop_name; }
        public void setShop_name(String shop_name) { this.shop_name = shop_name; }
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
    }

    // Route
    public static class RouteInfo {
        private List<ShopInfo> shops;
        private double total_distance;
        private int estimated_time;

        public List<ShopInfo> getShops() { return shops; }
        public void setShops(List<ShopInfo> shops) { this.shops = shops; }
        public double getTotal_distance() { return total_distance; }
        public void setTotal_distance(double total_distance) { this.total_distance = total_distance; }
        public int getEstimated_time() { return estimated_time; }
        public void setEstimated_time(int estimated_time) { this.estimated_time = estimated_time; }
    }

    public static class ShopInfo {
        private Long id;
        private String name;
        private String address;
        private double latitude;
        private double longitude;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }

    public WeatherInfo getWeather() { return weather; }
    public void setWeather(WeatherInfo weather) { this.weather = weather; }
    public RecipeInfo getRecipe() { return recipe; }
    public void setRecipe(RecipeInfo recipe) { this.recipe = recipe; }
    public List<ProductInfo> getProducts() { return products; }
    public void setProducts(List<ProductInfo> products) { this.products = products; }
    public RouteInfo getRoute() { return route; }
    public void setRoute(RouteInfo route) { this.route = route; }
    public List<RecipeInfo> getAlternative_recipes() { return alternative_recipes; }
    public void setAlternative_recipes(List<RecipeInfo> alternative_recipes) { this.alternative_recipes = alternative_recipes; }
}
