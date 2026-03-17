package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.model.Product;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvTrending, rvRecommendations, rvCategories, rvAutocomplete;
    private View dividerAutocomplete;
    private ApiService apiService;

    // Debounce: chỉ gọi API khi user ngừng gõ 300ms
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private Call<List<Product>> currentAutocompleteCall;

    // Autocomplete
    private AutocompleteAdapter autocompleteAdapter;
    private final List<Product> autocompleteSuggestions = new ArrayList<>();

    // Data phù hợp search sản phẩm BHX
    private static final String[] TRENDING_ITEMS = {"Thịt heo", "Rau xanh", "Trứng gà", "Mì gói"};
    private static final String[] RECENT_ITEMS = {"Thịt bò", "Nước mắm"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        apiService = ApiClient.getApiService(this);

        etSearch = findViewById(R.id.etSearch);
        rvTrending = findViewById(R.id.rvTrending);
        rvRecommendations = findViewById(R.id.rvRecommendations);
        rvCategories = findViewById(R.id.rvCategories);
        rvAutocomplete = findViewById(R.id.rvAutocomplete);
        dividerAutocomplete = findViewById(R.id.dividerAutocomplete);

        setupAutocomplete();
        setupSearchBar();
        setupTrendingChips();
        setupRecentSearches();
        setupRecommendations();
        setupCategories();
    }

    // ═══════════════════════════════════════════
    // SEARCH BAR + DEBOUNCE AUTOCOMPLETE
    // ═══════════════════════════════════════════

    private void setupSearchBar() {
        // Enter/Search key → full hybrid search
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(etSearch.getText().toString().trim());
            return true;
        });

        // TextWatcher + Debounce 300ms → autocomplete
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                // Cancel pending debounce
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                if (query.length() < 2) {
                    hideAutocomplete();
                    return;
                }

                // Debounce 300ms
                debounceRunnable = () -> fetchAutocomplete(query);
                debounceHandler.postDelayed(debounceRunnable, 300);
            }
        });
    }

    private void setupAutocomplete() {
        autocompleteAdapter = new AutocompleteAdapter(autocompleteSuggestions, product -> {
            // Click gợi ý → search full cho sản phẩm đó
            etSearch.setText(product.getName());
            etSearch.setSelection(product.getName().length());
            performSearch(product.getName());
        });
        rvAutocomplete.setLayoutManager(new LinearLayoutManager(this));
        rvAutocomplete.setAdapter(autocompleteAdapter);
    }

    private void fetchAutocomplete(String query) {
        // Cancel previous call
        if (currentAutocompleteCall != null) {
            currentAutocompleteCall.cancel();
        }

        currentAutocompleteCall = apiService.autocomplete(query);
        currentAutocompleteCall.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    autocompleteSuggestions.clear();
                    autocompleteSuggestions.addAll(response.body());
                    autocompleteAdapter.notifyDataSetChanged();
                    showAutocomplete();
                } else {
                    hideAutocomplete();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (!call.isCanceled()) {
                    hideAutocomplete();
                }
            }
        });
    }

    private void showAutocomplete() {
        rvAutocomplete.setVisibility(View.VISIBLE);
        if (dividerAutocomplete != null) dividerAutocomplete.setVisibility(View.VISIBLE);
    }

    private void hideAutocomplete() {
        rvAutocomplete.setVisibility(View.GONE);
        if (dividerAutocomplete != null) dividerAutocomplete.setVisibility(View.GONE);
        autocompleteSuggestions.clear();
        autocompleteAdapter.notifyDataSetChanged();
    }

    // ═══════════════════════════════════════════
    // TRENDING / RECENT / RECOMMENDATIONS / CATEGORIES
    // ═══════════════════════════════════════════

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
        list.add(new FoodRecommendationItem("Thịt heo nạc vai", "Thịt tươi ngon, đảm bảo ATVSTP", "45.900đ", "BHX", null));
        list.add(new FoodRecommendationItem("Trứng gà ta", "Trứng gà sạch, giàu dinh dưỡng", "32.000đ", "BHX", null));
        list.add(new FoodRecommendationItem("Rau muống", "Rau tươi, an toàn vệ sinh", "8.900đ", "BHX", null));

        rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecommendations.setAdapter(new FoodRecommendationAdapter(list, item -> applySearchAndRun(item.name)));
    }

    private void setupCategories() {
        List<CategoryItem> list = new ArrayList<>();
        list.add(new CategoryItem("Thịt heo", R.drawable.ic_food));
        list.add(new CategoryItem("Rau củ quả", R.drawable.ic_food));
        list.add(new CategoryItem("Hải sản", R.drawable.ic_food));
        list.add(new CategoryItem("Gia vị", R.drawable.ic_drink));

        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(new CategoryAdapter(list, item -> applySearchAndRun(item.name)));
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        hideAutocomplete();
        // Tìm kiếm sản phẩm - hiển thị kết quả trong CommunityFeed (tìm bài đăng liên quan)
        Intent intent = new Intent(SearchActivity.this, CommunityFeedActivity.class);
        intent.putExtra("SEARCH_QUERY", query.trim());
        startActivity(intent);
    }

    // ═══════════════════════════════════════════
    // AUTOCOMPLETE ADAPTER
    // ═══════════════════════════════════════════

    static class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.VH> {
        private final List<Product> items;
        private final OnSuggestionClick listener;

        interface OnSuggestionClick { void onClick(Product product); }

        AutocompleteAdapter(List<Product> items, OnSuggestionClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_autocomplete, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Product p = items.get(position);
            holder.tvName.setText(p.getName());
            holder.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "");
            holder.itemView.setOnClickListener(v -> listener.onClick(p));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAutoName);
                tvCategory = itemView.findViewById(R.id.tvAutoCategory);
            }
        }
    }

    // ═══════════════════════════════════════════
    // EXISTING ADAPTERS
    // ═══════════════════════════════════════════

    static class TrendingChipAdapter extends RecyclerView.Adapter<TrendingChipAdapter.VH> {
        private final List<String> items;
        private final OnChipClick listener;
        interface OnChipClick { void onChip(String text); }
        TrendingChipAdapter(List<String> items, OnChipClick listener) { this.items = items; this.listener = listener; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_chip, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            String text = items.get(position);
            holder.tv.setText(text);
            holder.itemView.setOnClickListener(v -> listener.onChip(text));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull View itemView) { super(itemView); tv = itemView.findViewById(R.id.tvTrendingChip); }
        }
    }

    static class FoodRecommendationItem {
        String name, description, price, rating, imageUrl;
        FoodRecommendationItem(String name, String description, String price, String rating, String imageUrl) {
            this.name = name; this.description = description; this.price = price; this.rating = rating; this.imageUrl = imageUrl;
        }
    }

    static class FoodRecommendationAdapter extends RecyclerView.Adapter<FoodRecommendationAdapter.VH> {
        private final List<FoodRecommendationItem> items;
        private final OnFoodClick listener;
        interface OnFoodClick { void onClick(FoodRecommendationItem item); }
        FoodRecommendationAdapter(List<FoodRecommendationItem> items, OnFoodClick listener) { this.items = items; this.listener = listener; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_recommendation_card, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            FoodRecommendationItem item = items.get(position);
            holder.tvName.setText(item.name); holder.tvDesc.setText(item.description);
            holder.tvPrice.setText(item.price); holder.tvRating.setText(item.rating);
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.iv.getContext()).load(item.imageUrl).centerCrop().placeholder(R.drawable.dish_placeholder).into(holder.iv);
            } else { holder.iv.setImageResource(R.drawable.dish_placeholder); }
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView iv; TextView tvName, tvDesc, tvPrice, tvRating;
            VH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.ivFoodImage); tvName = itemView.findViewById(R.id.tvFoodName);
                tvDesc = itemView.findViewById(R.id.tvFoodDescription); tvPrice = itemView.findViewById(R.id.tvFoodPrice);
                tvRating = itemView.findViewById(R.id.tvFoodRating);
            }
        }
    }

    static class CategoryItem {
        String name; int iconRes;
        CategoryItem(String name, int iconRes) { this.name = name; this.iconRes = iconRes; }
    }

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        private final List<CategoryItem> items;
        private final OnCategoryClick listener;
        interface OnCategoryClick { void onClick(CategoryItem item); }
        CategoryAdapter(List<CategoryItem> items, OnCategoryClick listener) { this.items = items; this.listener = listener; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_category, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryItem item = items.get(position);
            holder.tv.setText(item.name); holder.iv.setImageResource(item.iconRes);
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView iv; TextView tv;
            VH(@NonNull View itemView) {
                super(itemView); iv = itemView.findViewById(R.id.ivCategoryIcon); tv = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}
