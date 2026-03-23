package com.example.gomarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import java.util.Arrays;
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
    private Spinner spinnerProvince;
    private LinearLayout provinceContainer;

    private TextView chipAll, chipNongSan, chipDacSan, chipRaoVat, chipGomChung;
    private TextView chipMienBac, chipMienTrung, chipMienNam;

    private ApiService apiService;
    private SessionManager session;
    private String currentCategory = null;
    private String currentRegion = null;
    private String currentProvince = null;
    private String currentSearchQuery = null;
    private int currentPage = 0;

    // Province data per region
    private Map<String, List<String>> provincesMap = new HashMap<>();
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_feed);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initProvincesData();
        initViews();
        setupRecyclerView();

        // Handle search query from SearchActivity
        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            currentSearchQuery = searchQuery;
            searchBarContainer.setVisibility(View.VISIBLE);
            etSearch.setText(searchQuery);
            searchPosts(searchQuery);
        } else {
            loadFeed();
        }
    }

    private void initProvincesData() {
        provincesMap.put("MIEN_BAC", Arrays.asList(
            "Tất cả", "Hà Nội", "Hải Phòng", "Quảng Ninh", "Bắc Giang", "Bắc Kạn", "Bắc Ninh",
            "Cao Bằng", "Điện Biên", "Hà Giang", "Hà Nam", "Hải Dương", "Hòa Bình",
            "Hưng Yên", "Lai Châu", "Lạng Sơn", "Lào Cai", "Nam Định", "Ninh Bình",
            "Phú Thọ", "Sơn La", "Thái Bình", "Thái Nguyên", "Tuyên Quang", "Vĩnh Phúc", "Yên Bái"
        ));
        provincesMap.put("MIEN_TRUNG", Arrays.asList(
            "Tất cả", "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Quảng Bình", "Quảng Trị",
            "Thừa Thiên Huế", "Đà Nẵng", "Quảng Nam", "Quảng Ngãi", "Bình Định",
            "Phú Yên", "Khánh Hòa", "Ninh Thuận", "Bình Thuận",
            "Kon Tum", "Gia Lai", "Đắk Lắk", "Đắk Nông", "Lâm Đồng"
        ));
        provincesMap.put("MIEN_NAM", Arrays.asList(
            "Tất cả", "TP. Hồ Chí Minh", "Bà Rịa - Vũng Tàu", "Bình Dương", "Bình Phước",
            "Đồng Nai", "Tây Ninh", "Long An", "Tiền Giang", "Bến Tre", "Trà Vinh",
            "Vĩnh Long", "Đồng Tháp", "An Giang", "Kiên Giang", "Cần Thơ",
            "Hậu Giang", "Sóc Trăng", "Bạc Liêu", "Cà Mau"
        ));
    }

    private void initViews() {
        rvFeed = findViewById(R.id.rvFeed);
        emptyState = findViewById(R.id.emptyState);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        etSearch = findViewById(R.id.etSearch);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        provinceContainer = findViewById(R.id.provinceContainer);

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
                if (!query.isEmpty()) {
                    currentSearchQuery = query;
                    resetCategoryChips();
                    resetRegionChips();
                    currentCategory = null;
                    currentRegion = null;
                    currentProvince = null;
                    provinceContainer.setVisibility(View.GONE);
                    searchPosts(query);
                }
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

        // Region chips
        setupRegionChip(chipMienBac, "MIEN_BAC");
        setupRegionChip(chipMienTrung, "MIEN_TRUNG");
        setupRegionChip(chipMienNam, "MIEN_NAM");

        // Province spinner
        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerInitialized) {
                    spinnerInitialized = true;
                    return;
                }
                String selected = (String) parent.getItemAtPosition(position);
                if ("Tất cả".equals(selected)) {
                    currentProvince = null;
                } else {
                    currentProvince = selected;
                }
                currentPage = 0;
                currentSearchQuery = null;
                loadFeed();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCategoryChip(TextView chip, String category) {
        chip.setOnClickListener(v -> {
            resetRegionChips();
            resetCategoryChips();
            chip.setBackgroundResource(R.drawable.bg_filter_active);
            chip.setTextColor(getColor(R.color.white));
            currentCategory = category;
            currentRegion = null;
            currentProvince = null;
            currentSearchQuery = null;
            provinceContainer.setVisibility(View.GONE);
            currentPage = 0;
            loadFeed();
        });
    }

    private void setupRegionChip(TextView chip, String region) {
        chip.setOnClickListener(v -> {
            resetCategoryChips();
            resetRegionChips();
            chip.setBackgroundResource(R.drawable.bg_filter_active);
            chip.setTextColor(getColor(R.color.white));
            currentRegion = region;
            currentCategory = null;
            currentProvince = null;
            currentSearchQuery = null;
            currentPage = 0;

            // Show province spinner for selected region
            showProvinceSpinner(region);
            loadFeed();
        });
    }

    private void showProvinceSpinner(String region) {
        List<String> provinces = provincesMap.get(region);
        if (provinces != null) {
            spinnerInitialized = false;
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, provinces);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProvince.setAdapter(spinnerAdapter);
            provinceContainer.setVisibility(View.VISIBLE);
        }
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
        apiService.getFeed(null, null, currentPage, currentCategory, currentRegion, currentProvince)
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
        if (currentSearchQuery != null) return; // No pagination for search
        currentPage++;
        apiService.getFeed(null, null, currentPage, currentCategory, currentRegion, currentProvince)
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
        if (currentSearchQuery == null) {
            loadFeed();
        }
    }
}
