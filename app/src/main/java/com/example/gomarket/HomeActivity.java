package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.PostFeedAdapter;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements PostFeedAdapter.OnPostActionListener {

    // Old header (giữ nguyên)
    private MaterialCardView btnNotification, searchBar, bannerSuggest;
    private MaterialCardView btnWallet;
    private TextView tvGreeting, tvAddress, tvWalletBalance;

    // New action cards
    private MaterialCardView btnCreateList, btnShopperMode;

    // Community feed
    private RecyclerView rvCommunityFeed;
    private LinearLayout emptyFeedState;
    private TextView tvSeeAllPosts;
    private PostFeedAdapter postAdapter;
    private List<CommunityPost> feedPosts = new ArrayList<>();

    // Floating active order
    private View floatingOrderCard;
    private TextView tvFloatingStatus, tvFloatingShopperName, tvFloatingOrderSummary;
    private TextView btnDismissOrder;
    private MaterialButton btnTrackOrder;
    private long activeOrderId = -1;
    private boolean floatingOrderDismissed = false;

    // Bottom nav (5 tabs)
    private LinearLayout navHome, navTasks, navChat, navProfile;
    private FrameLayout navPost;

    // Services
    private ApiService apiService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadUserGreeting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserGreeting();
        loadWalletBalance();
        loadCommunityFeed();
        floatingOrderDismissed = false;
        loadActiveOrder();
    }

    private void initViews() {
        // Old header
        tvGreeting = findViewById(R.id.tvGreeting);
        tvAddress = findViewById(R.id.tvAddress);
        btnWallet = findViewById(R.id.btnWallet);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        btnNotification = findViewById(R.id.btnNotification);
        searchBar = findViewById(R.id.searchBar);
        bannerSuggest = findViewById(R.id.bannerSuggest);

        // Action cards
        btnCreateList = findViewById(R.id.btnCreateList);
        btnShopperMode = findViewById(R.id.btnShopperMode);

        // Community feed
        rvCommunityFeed = findViewById(R.id.rvCommunityFeed);
        emptyFeedState = findViewById(R.id.emptyFeedState);
        tvSeeAllPosts = findViewById(R.id.tvSeeAllPosts);

        // Floating order card
        floatingOrderCard = findViewById(R.id.floatingOrderCard);
        tvFloatingStatus = findViewById(R.id.tvFloatingStatus);
        tvFloatingShopperName = findViewById(R.id.tvFloatingShopperName);
        tvFloatingOrderSummary = findViewById(R.id.tvFloatingOrderSummary);
        btnDismissOrder = findViewById(R.id.btnDismissOrder);
        btnTrackOrder = findViewById(R.id.btnTrackOrder);

        // Bottom nav
        navHome = findViewById(R.id.navHome);
        navTasks = findViewById(R.id.navTasks);
        navPost = findViewById(R.id.navPost);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);
    }

    private void setupRecyclerView() {
        postAdapter = new PostFeedAdapter(this, feedPosts, this);
        rvCommunityFeed.setLayoutManager(new LinearLayoutManager(this));
        rvCommunityFeed.setAdapter(postAdapter);
    }

    private void setupClickListeners() {
        // Header
        searchBar.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, CookbookActivity.class)));
        btnWallet.setOnClickListener(v ->
                startActivity(new Intent(this, WalletActivity.class)));

        // AI Chef banner
        bannerSuggest.setOnClickListener(v ->
                startActivity(new Intent(this, AIChefActivity.class)));

        // Action cards
        btnCreateList.setOnClickListener(v ->
                startActivity(new Intent(this, CreateShoppingRequestActivity.class)));
        btnShopperMode.setOnClickListener(v ->
                startActivity(new Intent(this, ShopperDashboardActivity.class)));

        // Community feed
        tvSeeAllPosts.setOnClickListener(v ->
                startActivity(new Intent(this, CommunityFeedActivity.class)));

        // Floating order card
        btnDismissOrder.setOnClickListener(v -> dismissFloatingOrder());
        btnTrackOrder.setOnClickListener(v -> {
            if (activeOrderId > 0) {
                Intent intent = new Intent(this, OrderWaitingActivity.class);
                intent.putExtra("REQUEST_ID", activeOrderId);
                startActivity(intent);
            }
        });
        floatingOrderCard.setOnClickListener(v -> {
            if (activeOrderId > 0) {
                Intent intent = new Intent(this, OrderWaitingActivity.class);
                intent.putExtra("REQUEST_ID", activeOrderId);
                startActivity(intent);
            }
        });

        // Bottom nav
        navHome.setOnClickListener(v -> { /* already home */ });
        navTasks.setOnClickListener(v ->
                startActivity(new Intent(this, OrderListActivity.class)));
        navPost.setOnClickListener(v -> showPostOptions());
        navChat.setOnClickListener(v ->
                startActivity(new Intent(this, ConversationListActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    // ═══ OLD HEADER LOGIC ═══

    private void loadUserGreeting() {
        String name = session.getUserName();
        if (name != null && !name.isEmpty() && !name.equals("Người dùng")) {
            tvGreeting.setText("Chào, " + name + "! 👋");
        } else {
            tvGreeting.setText("Chào bạn! 👋");
        }
    }

    // ═══ WALLET ═══

    private void loadWalletBalance() {
        long userId = session.getUserId();
        if (userId <= 0) return;

        apiService.getWalletBalance(userId).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    tvWalletBalance.setText("💳 " + formatBalance(balance));
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvWalletBalance.setText("💳 --");
            }
        });
    }

    private String formatBalance(long balance) {
        if (balance >= 1_000_000) {
            return String.format("%.1fM", balance / 1_000_000.0);
        } else if (balance >= 1_000) {
            return String.format("%dK", balance / 1_000);
        }
        return balance + "đ";
    }

    // ═══ COMMUNITY FEED ═══

    private void loadCommunityFeed() {
        apiService.getFeed(null, null, 0, null).enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call,
                                   Response<List<CommunityPost>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    feedPosts.clear();
                    feedPosts.addAll(response.body());
                    postAdapter.updatePosts(feedPosts);
                }
                updateFeedEmptyState();
            }

            @Override
            public void onFailure(Call<List<CommunityPost>> call, Throwable t) {
                updateFeedEmptyState();
            }
        });
    }

    private void updateFeedEmptyState() {
        emptyFeedState.setVisibility(feedPosts.isEmpty() ? View.VISIBLE : View.GONE);
        rvCommunityFeed.setVisibility(feedPosts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ═══ FLOATING ACTIVE ORDER ═══

    private void loadActiveOrder() {
        if (floatingOrderDismissed) return;

        long userId = session.getUserId();
        if (userId <= 0) return;

        String role = session.getUserRole();
        Call<List<ShoppingRequest>> call;
        if ("SHOPPER".equalsIgnoreCase(role)) {
            call = apiService.getShopperRequests(userId);
        } else {
            call = apiService.getUserShoppingRequests(userId);
        }

        call.enqueue(new Callback<List<ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<ShoppingRequest>> call,
                                   Response<List<ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShoppingRequest active = null;
                    for (ShoppingRequest req : response.body()) {
                        String status = req.getStatus();
                        if (status != null
                                && !"COMPLETED".equals(status)
                                && !"CANCELLED".equals(status)) {
                            active = req;
                            break;
                        }
                    }
                    if (active != null && !floatingOrderDismissed) {
                        showFloatingOrder(active);
                    } else {
                        floatingOrderCard.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ShoppingRequest>> call, Throwable t) {
                floatingOrderCard.setVisibility(View.GONE);
            }
        });
    }

    private void showFloatingOrder(ShoppingRequest req) {
        activeOrderId = req.getId();
        floatingOrderCard.setVisibility(View.VISIBLE);

        String statusText;
        switch (req.getStatus()) {
            case "OPEN":
                statusText = "🟡 Đang chờ shopper";
                break;
            case "ACCEPTED":
                statusText = "🟢 Đã có shopper nhận";
                break;
            case "SHOPPING":
                statusText = "🛒 Đang đi chợ";
                break;
            case "DELIVERING":
                statusText = "🚗 Đang giao hàng";
                break;
            default:
                statusText = req.getStatus();
                break;
        }
        tvFloatingStatus.setText(statusText);
        tvFloatingShopperName.setText(
                req.getShopperName() != null ? req.getShopperName() : "Chờ nhận đơn...");
        tvFloatingOrderSummary.setText(
                req.getItemCount() + " sản phẩm · Đơn #" + String.format("%03d", req.getId()));
    }

    private void dismissFloatingOrder() {
        floatingOrderDismissed = true;
        floatingOrderCard.setVisibility(View.GONE);
    }

    // ═══ BOTTOM SHEET POST OPTIONS ═══

    private void showPostOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_post_options, null);

        sheetView.findViewById(R.id.optCreatePost).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CreatePostActivity.class));
        });

        sheetView.findViewById(R.id.optCreateRequest).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CreateShoppingRequestActivity.class));
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    // ═══ POST FEED ADAPTER CALLBACKS ═══

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
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                            int count = ((Number) response.body().get("likeCount")).intValue();
                            post.setIsLikedByUser(liked);
                            post.setLikeCount(count);
                            postAdapter.notifyItemChanged(position);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    }
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
            intent.setData(android.net.Uri.parse("tel:" + post.getAuthorPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không có thông tin liên hệ", Toast.LENGTH_SHORT).show();
        }
    }
}
