package com.example.gomarket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ShopperAdapter;
import com.example.gomarket.model.ShopperModel;

import java.util.ArrayList;
import java.util.List;

public class SelectShopperActivity extends AppCompatActivity {

    private RecyclerView rvShoppers;
    private ProgressBar progressBar;
    private ShopperAdapter adapter;
    private List<ShopperModel> shopperList;
    private String recipeId;
    private ArrayList<String> missingIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_shopper);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvShoppers = findViewById(R.id.rvShoppers);
        progressBar = findViewById(R.id.progressBar);

        // Get data from intent
        recipeId = getIntent().getStringExtra("RECIPE_ID");
        missingIngredients = getIntent().getStringArrayListExtra("MISSING_INGREDIENTS");

        if (missingIngredients != null && !missingIngredients.isEmpty()) {
            Toast.makeText(this, "Đang tìm shopper mua: " + String.join(", ", missingIngredients), Toast.LENGTH_LONG).show();
        }

        rvShoppers.setLayoutManager(new LinearLayoutManager(this));

        // Simulate Loading
        progressBar.setVisibility(View.VISIBLE);
        rvShoppers.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(this::loadMockShoppers, 1000);
    }

    private void loadMockShoppers() {
        shopperList = new ArrayList<>();

        // Mock Online Shoppers sorted by distance and rating
        shopperList.add(new ShopperModel("s1", "Nguyễn Minh", "https://example.com/avatar1.jpg", 4.9f, 320, 1.2, true, "Xe máy"));
        shopperList.add(new ShopperModel("s2", "Trần Hải", "https://example.com/avatar2.jpg", 4.8f, 230, 2.1, true, "Xe đạp"));
        shopperList.add(new ShopperModel("s3", "Lê Yến", "https://example.com/avatar3.jpg", 4.7f, 150, 3.5, true, "Xe máy"));
        shopperList.add(new ShopperModel("s4", "Phạm Hùng", "https://example.com/avatar4.jpg", 4.5f, 89, 4.0, false, "Xe đạp"));

        progressBar.setVisibility(View.GONE);
        rvShoppers.setVisibility(View.VISIBLE);

        int count = shopperList.size();
        android.widget.TextView tvCount = findViewById(R.id.tvShopperCount);
        if (tvCount != null) tvCount.setText(count + " shopper đang hoạt động trong 2km");

        adapter = new ShopperAdapter(this, shopperList, recipeId, missingIngredients);
        rvShoppers.setAdapter(adapter);
    }
}
