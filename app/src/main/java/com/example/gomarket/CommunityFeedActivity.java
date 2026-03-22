package com.example.gomarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.PostFeedAdapter;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityFeedActivity extends AppCompatActivity
        implements PostFeedAdapter.OnPostActionListener {

    private RecyclerView rvFeed;
    private PostFeedAdapter adapter;
    private List<CommunityPost> posts = new ArrayList<>();
    private LinearLayout emptyState;
    private MaterialCardView searchBarContainer;
    private EditText etSearch;

    private TextView chipAll, chipNongSan, chipDacSan, chipRaoVat, chipGomChung;
    private TextView chipMienBac, chipMienTrung, chipMienNam;

    private ApiService apiService;
    private SessionManager session;
    private String currentCategory = null;
    private String currentRegion = null;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_feed);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initViews();
        setupRecyclerView();
        loadFeed();
    }

    private void initViews() {
        rvFeed = findViewById(R.id.rvFeed);
        emptyState = findViewById(R.id.emptyState);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        etSearch = findViewById(R.id.etSearch);

        chipAll = findViewById(R.id.chipAll);
        chipNongSan = findViewById(R.id.chipNongSan);
        chipDacSan = findViewById(R.id.chipDacSan);
        chipRaoVat = findViewById(R.id.chipRaoVat);
        chipGomChung = findViewById(R.id.chipGomChung);
        chipMienBac = findViewById(R.id.chipMienBac);
        chipMienTrung = findViewById(R.id.chipMienTrung);
        chipMienNam = findViewById(R.id.chipMienNam);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabCreatePost).setOnClickListener(v -> {
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            boolean visible = searchBarContainer.getVisibility() == View.VISIBLE;
            searchBarContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            if (!visible) etSearch.requestFocus();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) searchPosts(query);
                return true;
            }
            return false;
        });

        // Category chips
        setupCategoryChip(chipAll, null);
        setupCategoryChip(chipNongSan, "nong_san");
        setupCategoryChip(chipDacSan, "dac_san");
        setupCategoryChip(chipRaoVat, "rao_vat");
        setupCategoryChip(chipGomChung, "gom_chung");

        // Region chips (vùng miền)
        setupRegionChip(chipMienBac, "MIEN_BAC");
        setupRegionChip(chipMienTrung, "MIEN_TRUNG");
        setupRegionChip(chipMienNam, "MIEN_NAM");
    }

    private void setupCategoryChip(TextView chip, String category) {
        chip.setOnClickListener(v -> {
            // Reset region chips
            resetRegionChips();
            // Highlight selected category chip
            resetCategoryChips();
            chip.setBackgroundResource(R.drawable.bg_filter_active);
            chip.setTextColor(getColor(R.color.white));
            currentCategory = category;
            currentRegion = null; // Clear region when selecting category
            currentPage = 0;
            loadFeed();
        });
    }

    private void setupRegionChip(TextView chip, String region) {
        chip.setOnClickListener(v -> {
            // Reset category chips
            resetCategoryChips();
            // Highlight selected region chip
            resetRegionChips();
            chip.setBackgroundResource(R.drawable.bg_filter_active);
            chip.setTextColor(getColor(R.color.white));
            currentRegion = region;
            currentCategory = null; // Clear category when selecting region
            currentPage = 0;
            loadFeed();
        });
    }

    private void resetCategoryChips() {
        chipAll.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipAll.setTextColor(getColor(R.color.text_secondary));
        chipNongSan.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipNongSan.setTextColor(getColor(R.color.text_secondary));
        chipDacSan.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipDacSan.setTextColor(getColor(R.color.text_secondary));
        chipRaoVat.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipRaoVat.setTextColor(getColor(R.color.text_secondary));
        chipGomChung.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipGomChung.setTextColor(getColor(R.color.text_secondary));
    }

    private void resetRegionChips() {
        chipMienBac.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipMienBac.setTextColor(getColor(R.color.text_secondary));
        chipMienTrung.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipMienTrung.setTextColor(getColor(R.color.text_secondary));
        chipMienNam.setBackgroundResource(R.drawable.bg_filter_inactive);
        chipMienNam.setTextColor(getColor(R.color.text_secondary));
    }

    private void setupRecyclerView() {
        adapter = new PostFeedAdapter(this, posts, this);
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        rvFeed.setAdapter(adapter);

        // Simple load more on scroll
        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= posts.size() - 2) {
                    loadMore();
                }
            }
        });
    }

    private void loadFeed() {
        apiService.getFeed(null, null, currentPage, currentCategory, currentRegion)
                .enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call, Response<List<CommunityPost>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    posts.clear();
                    posts.addAll(response.body());
                    adapter.updatePosts(posts);
                }
                updateEmptyState();
            }
            @Override
            public void onFailure(Call<List<CommunityPost>> call, Throwable t) {
                updateEmptyState();
            }
        });
    }

    private void loadMore() {
        currentPage++;
        apiService.getFeed(null, null, currentPage, currentCategory, currentRegion)
                .enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call, Response<List<CommunityPost>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    adapter.addPosts(response.body());
                }
            }
            @Override public void onFailure(Call<List<CommunityPost>> call, Throwable t) {}
        });
    }

    private void searchPosts(String query) {
        apiService.searchPosts(query).enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call, Response<List<CommunityPost>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    posts.clear();
                    posts.addAll(response.body());
                    adapter.updatePosts(posts);
                }
                updateEmptyState();
            }
            @Override
            public void onFailure(Call<List<CommunityPost>> call, Throwable t) {
                Toast.makeText(CommunityFeedActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        emptyState.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        rvFeed.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // PostFeedAdapter callbacks
    @Override
    public void onPostClick(CommunityPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.getId());
        startActivity(intent);
    }

    @Override
    public void onLikeClick(CommunityPost post, int position) {
        apiService.toggleLike(post.getId(), session.getUserId())
                .enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                    int count = ((Number) response.body().get("likeCount")).intValue();
                    post.setIsLikedByUser(liked);
                    post.setLikeCount(count);
                    adapter.notifyItemChanged(position);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    @Override
    public void onCommentClick(CommunityPost post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.getId());
        intent.putExtra("FOCUS_COMMENT", true);
        startActivity(intent);
    }

    @Override
    public void onContactClick(CommunityPost post) {
        if (post.getAuthorPhone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + post.getAuthorPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không có thông tin liên hệ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeed();
    }
}
