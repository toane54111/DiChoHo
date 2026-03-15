package com.example.gomarket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvTrending, rvRecommendations, rvCategories;

    private static final String[] TRENDING_ITEMS = {"Bún bò", "Cơm tấm", "Bánh mì", "Trà sữa"};
    private static final String[] RECENT_ITEMS = {"Thịt kho tàu", "Mì Ý"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearch);
        rvTrending = findViewById(R.id.rvTrending);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        rvCategories = findViewById(R.id.rvCategories);

        setupSearchBar();
        setupTrendingChips();
        setupRecentSearches();
        setupRecommendations();
        setupCategories();
    }

    private void setupSearchBar() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(etSearch.getText().toString().trim());
            return true;
        });
    }

    private void setupTrendingChips() {
        rvTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTrending.setAdapter(new TrendingChipAdapter(Arrays.asList(TRENDING_ITEMS), chipText -> {
            etSearch.setText(chipText);
            etSearch.setSelection(chipText.length());
            etSearch.requestFocus();
            performSearch(chipText);
        }));
    }

    private void setupRecentSearches() {
        View include1 = findViewById(R.id.includeRecent1);
        View include2 = findViewById(R.id.includeRecent2);
        if (include1 != null) {
            TextView tv1 = include1.findViewById(R.id.tvRecentSearchText);
            if (tv1 != null) tv1.setText(RECENT_ITEMS[0]);
            include1.setOnClickListener(v -> applySearchAndRun(RECENT_ITEMS[0]));
        }
        if (include2 != null) {
            TextView tv2 = include2.findViewById(R.id.tvRecentSearchText);
            if (tv2 != null) tv2.setText(RECENT_ITEMS[1]);
            include2.setOnClickListener(v -> applySearchAndRun(RECENT_ITEMS[1]));
        }
    }

    private void applySearchAndRun(String query) {
        etSearch.setText(query);
        etSearch.setSelection(query.length());
        performSearch(query);
    }

    private void setupRecommendations() {
        List<FoodRecommendationItem> list = new ArrayList<>();
        list.add(new FoodRecommendationItem("Phở Bò Gia Truyền", "Phở bò nấu truyền thống, tiệm đúng chất phở...", "30.000đ", "4.5", null));
        list.add(new FoodRecommendationItem("Phở Mà Mà Lài", "Phở bò thơm ngon, nước dùng đậm đà...", "35.000đ", "4.8", null));
        list.add(new FoodRecommendationItem("Tiến Bò Nướng", "Bò nướng mềm, ăn kèm bún...", "55.000đ", "4.6", null));

        rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecommendations.setAdapter(new FoodRecommendationAdapter(list, item -> applySearchAndRun(item.name)));
    }

    private void setupCategories() {
        List<CategoryItem> list = new ArrayList<>();
        list.add(new CategoryItem("Đồ ăn nhanh", R.drawable.ic_food));
        list.add(new CategoryItem("Tráng miệng", R.drawable.ic_food));
        list.add(new CategoryItem("Đồ uống", R.drawable.ic_drink));
        list.add(new CategoryItem("Healthy", R.drawable.ic_food));

        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(new CategoryAdapter(list, item -> applySearchAndRun(item.name)));
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đang tìm kiếm: " + query, Toast.LENGTH_SHORT).show();
    }

    // --- Adapters (reusable UI components) ---

    static class TrendingChipAdapter extends RecyclerView.Adapter<TrendingChipAdapter.VH> {
        private final List<String> items;
        private final OnChipClick listener;

        interface OnChipClick { void onChip(String text); }

        TrendingChipAdapter(List<String> items, OnChipClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_chip, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String text = items.get(position);
            holder.tv.setText(text);
            holder.itemView.setOnClickListener(v -> listener.onChip(text));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tvTrendingChip);
            }
        }
    }

    static class FoodRecommendationItem {
        String name, description, price, rating;
        String imageUrl;

        FoodRecommendationItem(String name, String description, String price, String rating, String imageUrl) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.rating = rating;
            this.imageUrl = imageUrl;
        }
    }

    static class FoodRecommendationAdapter extends RecyclerView.Adapter<FoodRecommendationAdapter.VH> {
        private final List<FoodRecommendationItem> items;
        private final OnFoodClick listener;

        interface OnFoodClick { void onClick(FoodRecommendationItem item); }

        FoodRecommendationAdapter(List<FoodRecommendationItem> items, OnFoodClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_recommendation_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FoodRecommendationItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.description);
            holder.tvPrice.setText(item.price);
            holder.tvRating.setText(item.rating);
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.iv.getContext()).load(item.imageUrl).centerCrop().placeholder(R.drawable.dish_placeholder).into(holder.iv);
            } else {
                holder.iv.setImageResource(R.drawable.dish_placeholder);
            }
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            TextView tvName, tvDesc, tvPrice, tvRating;

            VH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.ivFoodImage);
                tvName = itemView.findViewById(R.id.tvFoodName);
                tvDesc = itemView.findViewById(R.id.tvFoodDescription);
                tvPrice = itemView.findViewById(R.id.tvFoodPrice);
                tvRating = itemView.findViewById(R.id.tvFoodRating);
            }
        }
    }

    static class CategoryItem {
        String name;
        int iconRes;

        CategoryItem(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        private final List<CategoryItem> items;
        private final OnCategoryClick listener;

        interface OnCategoryClick { void onClick(CategoryItem item); }

        CategoryAdapter(List<CategoryItem> items, OnCategoryClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_category, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryItem item = items.get(position);
            holder.tv.setText(item.name);
            holder.iv.setImageResource(item.iconRes);
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            TextView tv;

            VH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.ivCategoryIcon);
                tv = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}
