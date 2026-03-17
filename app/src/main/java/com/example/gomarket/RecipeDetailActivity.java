package com.example.gomarket;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.IngredientAdapter;
import com.example.gomarket.database.RecipeDbHelper;
import com.example.gomarket.model.Recipe;
import com.example.gomarket.provider.RecipeContentProvider;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    private ImageView imgRecipe;
    private TextView tvRecipeName, tvWeatherContext, tvDescription, tvTotalCost;
    private TextView btnBack, btnSave;
    private RecyclerView recyclerViewIngredients;
    private LinearLayout layoutSteps;
    private MaterialButton btnBuyIngredients;

    private Recipe recipe;
    private boolean isSaved = false;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        initViews();
        loadRecipeData();
        setupClickListeners();
    }

    private void initViews() {
        imgRecipe = findViewById(R.id.imgRecipe);
        tvRecipeName = findViewById(R.id.tvRecipeName);
        tvWeatherContext = findViewById(R.id.tvWeatherContext);
        tvDescription = findViewById(R.id.tvDescription);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        recyclerViewIngredients = findViewById(R.id.recyclerViewIngredients);
        layoutSteps = findViewById(R.id.layoutSteps);
        btnBuyIngredients = findViewById(R.id.btnBuyIngredients);
    }

    private void loadRecipeData() {
        // Nhận data từ Intent (JSON string) - có thể là RecipeResponse hoặc Recipe
        String recipeJson = getIntent().getStringExtra("recipe_json");
        if (recipeJson != null) {
            try {
                // Thử parse như RecipeResponse trước (từ AIChefActivity)
                com.example.gomarket.model.RecipeResponse fullResponse =
                        gson.fromJson(recipeJson, com.example.gomarket.model.RecipeResponse.class);
                if (fullResponse != null && fullResponse.getRecipe() != null) {
                    recipe = fullResponse.getRecipe();
                    // Map matched products vào ingredients nếu có
                    if (fullResponse.getProducts() != null && recipe.getIngredients() != null) {
                        java.util.List<com.example.gomarket.model.Product> products = fullResponse.getProducts();
                        for (Recipe.Ingredient ing : recipe.getIngredients()) {
                            for (com.example.gomarket.model.Product p : products) {
                                if (p.getName().toLowerCase().contains(ing.getName().toLowerCase())
                                        || ing.getName().toLowerCase().contains(p.getName().toLowerCase())) {
                                    ing.setMatchedProduct(p);
                                    break;
                                }
                            }
                        }
                        // Tính tổng chi phí từ products nếu chưa có
                        if (recipe.getTotalCost() <= 0) {
                            double sum = 0;
                            for (com.example.gomarket.model.Product p : products) {
                                sum += p.getPrice();
                            }
                            recipe.setTotalCost(sum);
                        }
                    }
                } else {
                    // Fallback: parse như Recipe đơn
                    recipe = gson.fromJson(recipeJson, Recipe.class);
                }
            } catch (Exception e) {
                // Fallback: parse như Recipe đơn
                try {
                    recipe = gson.fromJson(recipeJson, Recipe.class);
                } catch (Exception ignored) {}
            }
        }

        if (recipe == null) {
            recipe = createDemoRecipe();
        }

        displayRecipe();
    }

    private Recipe createDemoRecipe() {
        Recipe demo = new Recipe();
        demo.setName("Lẩu Thái Hải Sản");
        demo.setDescription("Món lẩu đậm đà, cay nồng với nước dùng chua cay đặc trưng Thái Lan. Rất phù hợp cho những ngày mưa lạnh.");
        demo.setWeatherContext("Mưa lạnh, 22°C");
        demo.setTotalCost(185000);

        List<Recipe.Ingredient> ingredients = new java.util.ArrayList<>();
        String[][] data = {
                {"Tôm sú", "300g"}, {"Mực ống", "200g"}, {"Nấm kim châm", "150g"},
                {"Rau muống", "200g"}, {"Sả", "3 cây"}, {"Lá chanh", "5 lá"},
                {"Ớt hiểm", "3 quả"}, {"Nước cốt dừa", "200ml"}
        };
        for (String[] d : data) {
            Recipe.Ingredient ing = new Recipe.Ingredient();
            ing.setName(d[0]);
            ing.setQuantity(d[1]);
            ingredients.add(ing);
        }
        demo.setIngredients(ingredients);

        List<String> steps = new java.util.ArrayList<>();
        steps.add("Sơ chế tôm, mực. Rửa sạch rau và nấm.");
        steps.add("Đập dập sả, thái lát. Xé nhỏ lá chanh.");
        steps.add("Đun nước dùng với sả, lá chanh, ớt, nước cốt dừa.");
        steps.add("Nêm nếm gia vị: nước mắm, đường, chanh.");
        steps.add("Cho tôm, mực vào nồi lẩu. Nhúng rau và nấm ăn kèm.");
        demo.setSteps(steps);

        return demo;
    }

    private void displayRecipe() {
        tvRecipeName.setText(recipe.getName());
        tvDescription.setText(recipe.getDescription());
        tvTotalCost.setText(recipe.getFormattedTotalCost());

        if (recipe.getWeatherContext() != null) {
            tvWeatherContext.setText("🌧 Gợi ý cho thời tiết " + recipe.getWeatherContext());
        }

        // Ingredients
        if (recipe.getIngredients() != null) {
            IngredientAdapter adapter = new IngredientAdapter(recipe.getIngredients());
            recyclerViewIngredients.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewIngredients.setAdapter(adapter);
        }

        // Steps
        if (recipe.getSteps() != null) {
            for (int i = 0; i < recipe.getSteps().size(); i++) {
                TextView stepView = new TextView(this);
                stepView.setText("Bước " + (i + 1) + ": " + recipe.getSteps().get(i));
                stepView.setTextSize(14);
                stepView.setTextColor(getResources().getColor(R.color.text_primary));
                stepView.setPadding(0, 8, 0, 8);
                stepView.setLineSpacing(0, 1.3f);
                layoutSteps.addView(stepView);
            }
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            isSaved = !isSaved;
            if (isSaved) {
                btnSave.setText("❤️");
                saveRecipeToProvider();
                Toast.makeText(this, "Đã lưu công thức!", Toast.LENGTH_SHORT).show();
            } else {
                btnSave.setText("♡");
                Toast.makeText(this, "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
            }
        });

        btnBuyIngredients.setOnClickListener(v -> {
            // Chuyển nguyên liệu thành đơn đi chợ hộ
            Intent intent = new Intent(this, CreateShoppingRequestActivity.class);
            if (recipe.getIngredients() != null) {
                StringBuilder items = new StringBuilder();
                for (Recipe.Ingredient ing : recipe.getIngredients()) {
                    if (items.length() > 0) items.append("||");
                    items.append(ing.getName());
                    if (ing.getQuantity() != null) items.append("::").append(ing.getQuantity());
                }
                intent.putExtra("ITEMS", items.toString());
            }
            if (recipe.getTotalCost() > 0) {
                intent.putExtra("BUDGET", (long) recipe.getTotalCost());
            }
            intent.putExtra("NOTE", "Nguyên liệu cho: " + recipe.getName());
            startActivity(intent);
        });
    }

    private void saveRecipeToProvider() {
        ContentValues values = new ContentValues();
        values.put(RecipeDbHelper.COLUMN_RECIPE_NAME, recipe.getName());
        values.put(RecipeDbHelper.COLUMN_DESCRIPTION, recipe.getDescription());
        values.put(RecipeDbHelper.COLUMN_INGREDIENTS, gson.toJson(recipe.getIngredients()));
        values.put(RecipeDbHelper.COLUMN_STEPS, gson.toJson(recipe.getSteps()));
        values.put(RecipeDbHelper.COLUMN_WEATHER_CONTEXT, recipe.getWeatherContext());
        values.put(RecipeDbHelper.COLUMN_IMAGE_URL, recipe.getImageUrl());
        values.put(RecipeDbHelper.COLUMN_IS_FAVORITE, 1);

        Uri uri = getContentResolver().insert(RecipeContentProvider.CONTENT_URI, values);
        if (uri != null) {
            android.util.Log.d("RecipeDetail", "Saved recipe to ContentProvider: " + uri);
        }
    }

}
