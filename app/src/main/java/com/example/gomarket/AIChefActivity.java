package com.example.gomarket;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gomarket.model.RecipeRequest;
import com.example.gomarket.model.Product;
import com.example.gomarket.model.Recipe;
import com.example.gomarket.model.RecipeResponse;
import com.example.gomarket.model.WeatherData;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.service.LocationService;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChefActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;

    // Header
    private MaterialCardView btnBack;

    // Weather views
    private ImageView imgWeatherBg;
    private TextView tvWeatherIcon, tvWeatherTemp, tvWeatherDesc, tvWeatherCity;

    // Recipe views
    private TextView tvRecipeName, tvRecipeDesc, tvTotalCost;
    private ImageView imgMonAn;
    private LinearLayout layoutIngredients;
    private TextView tvIngredientLoading;

    // Buttons
    private MaterialButton btnThemVaoGio;
    private MaterialButton btnDoiMon;

    // Data
    private double currentLat = 10.7769;
    private double currentLng = 106.7009;
    private RecipeResponse currentResponse;
    private ApiService apiService;
    private BroadcastReceiver locationReceiver;
    private boolean hasReceivedLocation = false;

    // Track excluded dishes for "change dish" feature
    private List<String> excludedDishes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chef);

        apiService = ApiClient.getApiService(this);
        initViews();
        setupClickListeners();
        requestLocationAndSuggest();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);

        // Weather
        imgWeatherBg = findViewById(R.id.imgWeatherBg);
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon);
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
        tvWeatherCity = findViewById(R.id.tvWeatherCity);

        // Recipe
        tvRecipeName = findViewById(R.id.tvRecipeName);
        tvRecipeDesc = findViewById(R.id.tvRecipeDesc);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        imgMonAn = findViewById(R.id.imgMonAn);
        layoutIngredients = findViewById(R.id.layoutIngredients);
        tvIngredientLoading = findViewById(R.id.tvIngredientLoading);

        // Buttons
        btnThemVaoGio = findViewById(R.id.btnThemVaoGio);
        btnDoiMon = findViewById(R.id.btnDoiMon);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnThemVaoGio.setOnClickListener(v -> {
            if (currentResponse != null && currentResponse.getRecipe() != null) {
                Intent intent = new Intent(this, RecipeDetailActivity.class);
                intent.putExtra("recipe_json", new Gson().toJson(currentResponse));
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, RecipeDetailActivity.class);
                startActivity(intent);
            }
        });

        btnDoiMon.setOnClickListener(v -> {
            if (currentResponse != null && currentResponse.getRecipe() != null) {
                String currentDish = currentResponse.getRecipe().getName();
                if (currentDish != null && !excludedDishes.contains(currentDish)) {
                    excludedDishes.add(currentDish);
                }
            }
            fetchRecipeSuggestion();
        });
    }

    private void requestLocationAndSuggest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            startLocationAndFetch();
        }
    }

    private void startLocationAndFetch() {
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationService.ACTION_LOCATION_UPDATED.equals(intent.getAction())) {
                    currentLat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, currentLat);
                    currentLng = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, currentLng);
                    if (!hasReceivedLocation) {
                        hasReceivedLocation = true;
                        fetchWeatherFirst();
                        fetchRecipeSuggestion();
                    }
                    try { unregisterReceiver(locationReceiver); } catch (Exception ignored) {}
                }
            }
        };

        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_UPDATED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }

        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Fetch weather ngay (nhanh), recipe chờ GPS xong mới gọi
        fetchWeatherFirst();

        // Fallback: nếu GPS quá lâu (5s), gọi recipe với tọa độ mặc định
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!hasReceivedLocation) {
                hasReceivedLocation = true;
                fetchRecipeSuggestion();
            }
        }, 5000);
    }

    /**
     * Gọi weather API riêng (nhanh) để hiện thời tiết trước
     */
    private void fetchWeatherFirst() {
        apiService.getWeather(currentLat, currentLng).enqueue(new Callback<WeatherData>() {
            @Override
            public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherData weather = response.body();
                    tvWeatherTemp.setText(String.format("%.0f°C", weather.getTemp()));
                    tvWeatherDesc.setText(capitalizeFirst(weather.getDescription()));
                    tvWeatherCity.setText(weather.getCity());
                    updateWeatherIcon(weather.getIcon());
                    updateWeatherBackground(weather.getIcon());
                }
            }

            @Override
            public void onFailure(Call<WeatherData> call, Throwable t) {
                // Weather fallback - sẽ được cập nhật khi recipe response về
            }
        });
    }

    private void fetchRecipeSuggestion() {
        tvRecipeName.setText("Dang goi y...");
        tvRecipeDesc.setText("AI dang phan tich thoi tiet va tim mon an phu hop cho ban...");
        btnThemVaoGio.setEnabled(false);
        btnDoiMon.setVisibility(View.GONE);

        // Reset dish image to default while loading
        Glide.with(this).clear(imgMonAn);
        imgMonAn.setImageResource(R.drawable.dish_default);

        RecipeRequest request = new RecipeRequest(currentLat, currentLng,
                excludedDishes.isEmpty() ? null : excludedDishes);

        apiService.suggestRecipe(request).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentResponse = response.body();
                    updateWeatherUI();
                    updateRecipeUI();
                    updateIngredientsUI();
                    btnThemVaoGio.setEnabled(true);
                    btnDoiMon.setVisibility(View.VISIBLE);
                } else {
                    showFallbackUI("Khong the goi y mon an (loi server)");
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                showFallbackUI("Khong the ket noi server");
            }
        });
    }

    // ─── UPDATE WEATHER CARD ───
    private void updateWeatherUI() {
        if (currentResponse == null || currentResponse.getWeather() == null) return;

        WeatherData weather = currentResponse.getWeather();

        tvWeatherTemp.setText(String.format("%.0f°C", weather.getTemp()));
        tvWeatherDesc.setText(capitalizeFirst(weather.getDescription()));
        tvWeatherCity.setText(weather.getCity());
        updateWeatherIcon(weather.getIcon());
        updateWeatherBackground(weather.getIcon());
    }

    private void updateWeatherIcon(String icon) {
        if (icon == null) return;
        if (icon.startsWith("01")) tvWeatherIcon.setText("☀️");
        else if (icon.startsWith("02")) tvWeatherIcon.setText("⛅");
        else if (icon.startsWith("03") || icon.startsWith("04")) tvWeatherIcon.setText("☁️");
        else if (icon.startsWith("09") || icon.startsWith("10")) tvWeatherIcon.setText("🌧️");
        else if (icon.startsWith("11")) tvWeatherIcon.setText("⛈️");
        else if (icon.startsWith("13")) tvWeatherIcon.setText("❄️");
        else if (icon.startsWith("50")) tvWeatherIcon.setText("🌫️");
    }

    private void updateWeatherBackground(String icon) {
        if (icon == null) return;
        if (icon.startsWith("01") || icon.startsWith("02")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_sunny);
        } else if (icon.startsWith("03") || icon.startsWith("04")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_cloudy);
        } else if (icon.startsWith("09") || icon.startsWith("10")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_rainy);
        } else if (icon.startsWith("11")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_storm);
        } else if (icon.startsWith("13")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_cold);
        } else if (icon.startsWith("50")) {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_foggy);
        } else {
            imgWeatherBg.setImageResource(R.drawable.bg_weather_default);
        }
    }

    // ─── UPDATE RECIPE CARD ───
    private void updateRecipeUI() {
        if (currentResponse == null || currentResponse.getRecipe() == null) return;

        Recipe recipe = currentResponse.getRecipe();
        tvRecipeName.setText(recipe.getName());
        tvRecipeDesc.setText(recipe.getDescription());

        // Dynamic dish image: prefer URL from backend, fallback to local drawable
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.dish_default)
                    .error(R.drawable.dish_default)
                    .into(imgMonAn);
        } else {
            updateDishImage(recipe.getName());
        }

        double totalCost = recipe.getTotalCost();
        if (totalCost > 0) {
            tvTotalCost.setText(String.format("~%,.0fd", totalCost));
        } else if (currentResponse.getProducts() != null) {
            double sum = 0;
            for (Product p : currentResponse.getProducts()) {
                sum += p.getPrice();
            }
            if (sum > 0) tvTotalCost.setText(String.format("~%,.0fd", sum));
        }
    }

    private void updateDishImage(String dishName) {
        if (dishName == null) {
            imgMonAn.setImageResource(R.drawable.dish_default);
            return;
        }
        String lower = dishName.toLowerCase();

        if (lower.contains("lau") || lower.contains("lẩu")) {
            imgMonAn.setImageResource(R.drawable.lauthai);
        } else if (lower.contains("canh chua")) {
            imgMonAn.setImageResource(R.drawable.canhchua);
        } else if (lower.contains("banh xeo") || lower.contains("bánh xèo")) {
            imgMonAn.setImageResource(R.drawable.banhxeo);
        } else if (lower.contains("pho") || lower.contains("phở")) {
            imgMonAn.setImageResource(R.drawable.phobo);
        } else {
            // Fallback: default food image
            imgMonAn.setImageResource(R.drawable.dish_default);
        }
    }

    // ─── UPDATE INGREDIENTS LIST ───
    private void updateIngredientsUI() {
        if (currentResponse == null || currentResponse.getRecipe() == null) return;

        Recipe recipe = currentResponse.getRecipe();
        List<Recipe.Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) return;

        layoutIngredients.removeAllViews();

        String[] emojis = {"🦐", "🦑", "🍄", "🥬", "🌶️", "🧅", "🥩", "🐟", "🥕", "🧄", "🍋", "🥥"};

        for (int i = 0; i < ingredients.size(); i++) {
            Recipe.Ingredient ingredient = ingredients.get(i);
            boolean hasMatch = ingredient.getMatchedProduct() != null;
            double price = hasMatch ? ingredient.getMatchedProduct().getPrice() : 0;
            addIngredientRow(emojis[i % emojis.length], ingredient.getName(),
                    ingredient.getQuantity(), hasMatch, price);
        }
    }

    private void addIngredientRow(String emoji, String name, String quantity,
                                   boolean available, double price) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        if (!available) {
            row.setAlpha(0.4f);
        }

        // Emoji
        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(20);
        row.addView(tvEmoji);

        // Name + quantity column
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMarginStart(dpToPx(12));
        nameCol.setLayoutParams(nameParams);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(14);
        tvName.setTextColor(available ? 0xFF212121 : 0xFF9E9E9E);
        if (!available) {
            tvName.setTypeface(null, Typeface.ITALIC);
        }
        nameCol.addView(tvName);

        TextView tvQty = new TextView(this);
        tvQty.setText(quantity);
        tvQty.setTextSize(12);
        tvQty.setTextColor(0xFF757575);
        nameCol.addView(tvQty);

        row.addView(nameCol);

        // Price or unavailable label
        TextView tvPrice = new TextView(this);
        if (available && price > 0) {
            tvPrice.setText(String.format("%,.0fd", price));
            tvPrice.setTextColor(0xFF4CAF50);
            tvPrice.setTextSize(14);
            tvPrice.setTypeface(null, Typeface.BOLD);
        } else {
            tvPrice.setText("Hết hàng");
            tvPrice.setTextColor(0xFFE53935);
            tvPrice.setTextSize(12);
            tvPrice.setTypeface(null, Typeface.ITALIC);
        }
        row.addView(tvPrice);

        layoutIngredients.addView(row);
    }

    private void showFallbackUI(String message) {
        tvRecipeName.setText("Lau Thai Hai San");
        tvRecipeDesc.setText("Mon lau chua cay dam da, phu hop cho ngay mua se lanh.");
        tvWeatherTemp.setText("28°C");
        tvWeatherDesc.setText("troi nang");
        tvWeatherCity.setText("TP. Ho Chi Minh");
        tvWeatherIcon.setText("☀️");
        imgWeatherBg.setImageResource(R.drawable.bg_weather_sunny);
        imgMonAn.setImageResource(R.drawable.lauthai);
        tvTotalCost.setText("~185.000d");
        btnThemVaoGio.setEnabled(true);
        btnDoiMon.setVisibility(View.VISIBLE);

        layoutIngredients.removeAllViews();
        String[][] defaultIngs = {
                {"🦐", "Tom su", "300g"}, {"🦑", "Muc ong", "200g"},
                {"🍄", "Nam kim cham", "150g"}, {"🥬", "Rau muong", "200g"},
                {"🌶️", "Sa", "3 cay"}, {"🧅", "La chanh", "5 la"},
                {"🌶️", "Ot hiem", "3 qua"}, {"🥥", "Nuoc cot dua", "200ml"}
        };
        for (String[] ing : defaultIngs) {
            addIngredientRow(ing[0], ing[1], ing[2], true, 0);
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationAndFetch();
            } else {
                Toast.makeText(this, "Can quyen vi tri de goi y mon an phu hop", Toast.LENGTH_LONG).show();
                fetchRecipeSuggestion();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (locationReceiver != null) unregisterReceiver(locationReceiver);
        } catch (Exception ignored) {}
        stopService(new Intent(this, LocationService.class));
    }
}
