package com.example.gomarket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.DishAdapter;
import com.example.gomarket.model.RecipeModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DishResultActivity extends AppCompatActivity {

    private RecyclerView rvDishResults;
    private TextView tvNoResult;
    private ProgressBar progressBar;
    private DishAdapter adapter;
    private List<RecipeModel> recipeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_result);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvDishResults = findViewById(R.id.rvDishResults);
        tvNoResult = findViewById(R.id.tvNoResult);
        progressBar = findViewById(R.id.progressBar);

        // Get Input from Search Activity
        ArrayList<String> userIngredients = getIntent().getStringArrayListExtra("USER_INGREDIENTS");
        if (userIngredients == null) {
            userIngredients = new ArrayList<>();
        }

        rvDishResults.setLayoutManager(new LinearLayoutManager(this));

        // Simulate Network Loading
        progressBar.setVisibility(View.VISIBLE);
        rvDishResults.setVisibility(View.GONE);

        ArrayList<String> finalUserIngredients = userIngredients;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadMockData(finalUserIngredients);
        }, 1500); // 1.5s delay to simulate search
    }

    private void loadMockData(List<String> userIngredients) {
        recipeList = new ArrayList<>();
        
        // Mock Recipes
        recipeList.add(new RecipeModel("r1", "Gà xào cà chua", "https://example.com/ga_ca_chua.jpg", 
                Arrays.asList("Gà", "Cà chua", "Hành"), 25));
        recipeList.add(new RecipeModel("r2", "Canh gà lá giang", "https://example.com/canh_ga.jpg", 
                Arrays.asList("Gà", "Lá giang", "Hành", "Ớt", "Tỏi"), 40));
        recipeList.add(new RecipeModel("r3", "Thịt bò xào hành tây", "https://example.com/bo_hanh_tay.jpg", 
                Arrays.asList("Thịt bò", "Hành tây", "Tỏi", "Tiêu"), 20));
        recipeList.add(new RecipeModel("r4", "Trứng chiên cà chua", "https://example.com/trung_ca_chua.jpg", 
                Arrays.asList("Trứng", "Cà chua", "Hành"), 10));

        // Filter recipes based on ingredients (mock logic: if it has at least 1 match)
        List<RecipeModel> matchedRecipes = new ArrayList<>();
        for (RecipeModel recipe : recipeList) {
            boolean hasMatch = false;
            for (String req : recipe.getIngredients()) {
                for (String user : userIngredients) {
                    if (req.toLowerCase().contains(user.toLowerCase()) || user.toLowerCase().contains(req.toLowerCase())) {
                        hasMatch = true;
                        break;
                    }
                }
                if (hasMatch) break;
            }
            if (hasMatch) {
                matchedRecipes.add(recipe);
            }
        }

        progressBar.setVisibility(View.GONE);

        if (matchedRecipes.isEmpty()) {
            tvNoResult.setVisibility(View.VISIBLE);
            rvDishResults.setVisibility(View.GONE);
        } else {
            tvNoResult.setVisibility(View.GONE);
            rvDishResults.setVisibility(View.VISIBLE);
            adapter = new DishAdapter(this, matchedRecipes, userIngredients);
            rvDishResults.setAdapter(adapter);
        }
    }
}
