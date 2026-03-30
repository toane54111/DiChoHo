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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvSuggestions;
    private View trendingSection, regionSection;

    private ApiService apiService;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private Call<List<CommunityPost>> currentSearchCall;

    private SuggestionAdapter suggestionAdapter;
    private final List<CommunityPost> suggestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        apiService = ApiClient.getApiService(this);

        etSearch = findViewById(R.id.etSearch);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        trendingSection = findViewById(R.id.trendingSection);
        regionSection = findViewById(R.id.regionSection);

        setupSearchBar();
        setupSuggestions();
        setupTrendingChips();
        setupRegionCards();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupSearchBar() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
            return true;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                if (query.length() < 2) {
                    hideSuggestions();
                    return;
                }
                debounceRunnable = () -> fetchSuggestions(query);
                debounceHandler.postDelayed(debounceRunnable, 300);
            }
        });
    }

    private void setupSuggestions() {
        suggestionAdapter = new SuggestionAdapter(suggestions, post -> {
            etSearch.setText(post.getTitle());
            performSearch(post.getTitle());
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void fetchSuggestions(String query) {
        if (currentSearchCall != null) currentSearchCall.cancel();

        currentSearchCall = apiService.searchPosts(query);
        currentSearchCall.enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call, Response<List<CommunityPost>> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    suggestions.clear();
                    // Show max 5 suggestions
                    List<CommunityPost> body = response.body();
                    suggestions.addAll(body.subList(0, Math.min(5, body.size())));
                    suggestionAdapter.notifyDataSetChanged();
                    showSuggestions();
                } else {
                    hideSuggestions();
                }
            }
            @Override
            public void onFailure(Call<List<CommunityPost>> call, Throwable t) {
                if (!call.isCanceled()) hideSuggestions();
            }
        });
    }

    private void showSuggestions() {
        rvSuggestions.setVisibility(View.VISIBLE);
        trendingSection.setVisibility(View.GONE);
        regionSection.setVisibility(View.GONE);
    }

    private void hideSuggestions() {
        rvSuggestions.setVisibility(View.GONE);
        suggestions.clear();
        suggestionAdapter.notifyDataSetChanged();
        trendingSection.setVisibility(View.VISIBLE);
        regionSection.setVisibility(View.VISIBLE);
    }

    private void setupTrendingChips() {
        String[] trending = {"Nông sản", "Đặc sản", "Gom chung", "Rao vặt",
                "Trái cây", "Hải sản", "Cà phê", "Mắm"};

        RecyclerView rvTrending = findViewById(R.id.rvTrending);
        rvTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTrending.setAdapter(new TrendingChipAdapter(trending, chip -> {
            etSearch.setText(chip);
            etSearch.setSelection(chip.length());
            performSearch(chip);
        }));
    }

    private void setupRegionCards() {
        findViewById(R.id.cardMienBac).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityFeedActivity.class);
            intent.putExtra("FILTER_REGION", "MIEN_BAC");
            startActivity(intent);
        });
        findViewById(R.id.cardMienTrung).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityFeedActivity.class);
            intent.putExtra("FILTER_REGION", "MIEN_TRUNG");
            startActivity(intent);
        });
        findViewById(R.id.cardMienNam).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityFeedActivity.class);
            intent.putExtra("FILTER_REGION", "MIEN_NAM");
            startActivity(intent);
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        hideSuggestions();
        Intent intent = new Intent(this, CommunityFeedActivity.class);
        intent.putExtra("SEARCH_QUERY", query.trim());
        startActivity(intent);
    }

    // ═══ Suggestion Adapter ═══
    static class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {
        private final List<CommunityPost> items;
        private final OnClick listener;
        interface OnClick { void onClick(CommunityPost post); }

        SuggestionAdapter(List<CommunityPost> items, OnClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_suggestion, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CommunityPost post = items.get(position);
            holder.tvTitle.setText(post.getTitle());
            String meta = post.getCategoryDisplay();
            if (post.getLocationName() != null) meta += " · " + post.getLocationName();
            holder.tvMeta.setText(meta);
            holder.itemView.setOnClickListener(v -> listener.onClick(post));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMeta;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvSuggestionTitle);
                tvMeta = itemView.findViewById(R.id.tvSuggestionMeta);
            }
        }
    }

    // ═══ Trending Chip Adapter ═══
    static class TrendingChipAdapter extends RecyclerView.Adapter<TrendingChipAdapter.VH> {
        private final String[] items;
        private final OnChipClick listener;
        interface OnChipClick { void onChip(String text); }

        TrendingChipAdapter(String[] items, OnChipClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trending_chip, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tv.setText(items[position]);
            holder.itemView.setOnClickListener(v -> listener.onChip(items[position]));
        }

        @Override public int getItemCount() { return items.length; }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull View itemView) { super(itemView); tv = itemView.findViewById(R.id.tvTrendingChip); }
        }
    }
}
