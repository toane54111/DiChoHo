package com.example.gomarket;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.example.gomarket.model.LocalGuideResponse;
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
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements PostFeedAdapter.OnPostActionListener {

    private MaterialCardView btnNotification, searchBar;
    private MaterialCardView btnWallet;
    private TextView tvGreeting, tvUserName, tvAddress, tvWalletBalance;

    private MaterialCardView btnCreateList, btnShopperMode;

    private RecyclerView rvCommunityFeed;
    private LinearLayout emptyFeedState;
    private TextView tvSeeAllPosts;
    private PostFeedAdapter postAdapter;
    private List<CommunityPost> feedPosts = new ArrayList<>();

    private View floatingOrderCard;
    private TextView tvFloatingStatus, tvFloatingShopperName, tvFloatingOrderSummary;
    private TextView btnDismissOrder;
    private MaterialButton btnTrackOrder;
    private long activeOrderId = -1;
    private boolean floatingOrderDismissed = false;

    private LinearLayout navHome, navTasks, navChat, navProfile;
    private FrameLayout navPost;

    // AI Thổ Địa — horizontal
    private LinearLayout containerSuggestionChips;
    private LinearLayout localGuideLoading;
    private TextView tvSeasonalLocationLine;
    private TextView tvSeasonalHeadline;
    private View cardSuggestionDetail;
    private TextView tvDetailEmoji, tvDetailName, tvDetailReason, tvDetailNoResults;
    private LinearLayout containerDetailPosts, listDetailPosts;
    private MaterialCardView btnDetailGomChung, btnDetailNhoMua;
    private TextView btnCloseDetail;

    private List<LocalGuideResponse.SuggestionItem> suggestionItems;
    private int selectedSuggestionIndex = -1;

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
        loadLocalGuideSuggestions();
        floatingOrderDismissed = false;
        loadActiveOrder();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserName = findViewById(R.id.tvUserName);
        tvAddress = findViewById(R.id.tvAddress);
        btnWallet = findViewById(R.id.btnWallet);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        btnNotification = findViewById(R.id.btnNotification);
        searchBar = findViewById(R.id.searchBar);

        // AI Thổ Địa
        containerSuggestionChips = findViewById(R.id.containerSuggestionChips);
        localGuideLoading = findViewById(R.id.localGuideLoading);
        tvSeasonalLocationLine = findViewById(R.id.tvSeasonalLocationLine);
        tvSeasonalHeadline = findViewById(R.id.tvSeasonalHeadline);
        cardSuggestionDetail = findViewById(R.id.cardSuggestionDetail);
        tvDetailEmoji = findViewById(R.id.tvDetailEmoji);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailReason = findViewById(R.id.tvDetailReason);
        tvDetailNoResults = findViewById(R.id.tvDetailNoResults);
        containerDetailPosts = findViewById(R.id.containerDetailPosts);
        listDetailPosts = findViewById(R.id.listDetailPosts);
        btnDetailGomChung = findViewById(R.id.btnDetailGomChung);
        btnDetailNhoMua = findViewById(R.id.btnDetailNhoMua);
        btnCloseDetail = findViewById(R.id.btnCloseDetail);

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
        searchBar.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, CookbookActivity.class)));
        btnWallet.setOnClickListener(v ->
                startActivity(new Intent(this, WalletActivity.class)));

        btnCreateList.setOnClickListener(v ->
                startActivity(new Intent(this, CreateShoppingRequestActivity.class)));
        btnShopperMode.setOnClickListener(v ->
                startActivity(new Intent(this, ShopperDashboardActivity.class)));

        tvSeeAllPosts.setOnClickListener(v ->
                startActivity(new Intent(this, CommunityFeedActivity.class)));

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

        btnCloseDetail.setOnClickListener(v -> {
            cardSuggestionDetail.setVisibility(View.GONE);
            selectedSuggestionIndex = -1;
            refreshChipSelection();
        });

        navHome.setOnClickListener(v -> { });
        navTasks.setOnClickListener(v ->
                startActivity(new Intent(this, OrderListActivity.class)));
        navPost.setOnClickListener(v -> showPostOptions());
        navChat.setOnClickListener(v ->
                startActivity(new Intent(this, ConversationListActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadUserGreeting() {
        tvGreeting.setText("Xin chào 👋");
        String name = session.getUserName();
        if (name != null && !name.isEmpty() && !name.equals("Người dùng")) {
            tvUserName.setText(name);
        } else {
            tvUserName.setText("bạn");
        }
    }

    private void loadWalletBalance() {
        long userId = session.getUserId();
        if (userId <= 0) return;

        apiService.getWalletBalance(userId).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    tvWalletBalance.setText("🪙 " + formatBalance(balance));
                }
            }
            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvWalletBalance.setText("🪙 --");
            }
        });
    }

    private String formatBalance(long balance) {
        if (balance >= 1_000_000) return String.format("%.1fM", balance / 1_000_000.0);
        else if (balance >= 1_000) return String.format("%dK", balance / 1_000);
        return balance + "đ";
    }

    // ═══ COMMUNITY FEED ═══

    private void loadCommunityFeed() {
        apiService.getFeed(null, null, 0, null, null, null).enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(Call<List<CommunityPost>> call, Response<List<CommunityPost>> response) {
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
            public void onResponse(Call<List<ShoppingRequest>> call, Response<List<ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShoppingRequest active = null;
                    for (ShoppingRequest req : response.body()) {
                        String status = req.getStatus();
                        if (status != null && !"COMPLETED".equals(status) && !"CANCELLED".equals(status)) {
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
            case "OPEN": statusText = "🟡 Đang chờ shopper"; break;
            case "ACCEPTED": statusText = "🟢 Đã có shopper nhận"; break;
            case "SHOPPING": statusText = "🛒 Đang đi chợ"; break;
            case "DELIVERING": statusText = "🚗 Đang giao hàng"; break;
            default: statusText = req.getStatus(); break;
        }
        tvFloatingStatus.setText(statusText);
        tvFloatingShopperName.setText(req.getShopperName() != null ? req.getShopperName() : "Chờ nhận đơn...");
        tvFloatingOrderSummary.setText(req.getItemCount() + " sản phẩm · Đơn #" + String.format("%03d", req.getId()));
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
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                            int count = ((Number) response.body().get("likeCount")).intValue();
                            post.setIsLikedByUser(liked);
                            post.setLikeCount(count);
                            postAdapter.notifyItemChanged(position);
                        }
                    }
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) { }
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

    // ═══ AI THỔ ĐỊA — Horizontal Slide Bar ═══

    private boolean localGuideLoaded = false;

    private void loadLocalGuideSuggestions() {
        if (localGuideLoaded) return;
        long userId = session.getUserId();
        if (userId <= 0) return;

        localGuideLoading.setVisibility(View.VISIBLE);
        containerSuggestionChips.setVisibility(View.GONE);

        apiService.getLocalGuideSuggestions(userId, 10.7769, 106.7009)
                .enqueue(new Callback<LocalGuideResponse>() {
            @Override
            public void onResponse(Call<LocalGuideResponse> call, Response<LocalGuideResponse> response) {
                localGuideLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    localGuideLoaded = true;
                    renderLocalGuideHorizontal(response.body());
                } else {
                    tvSeasonalLocationLine.setText("");
                    tvSeasonalHeadline.setText("Không thể tải gợi ý. Thử lại sau.");
                }
            }
            @Override
            public void onFailure(Call<LocalGuideResponse> call, Throwable t) {
                localGuideLoading.setVisibility(View.GONE);
                tvSeasonalLocationLine.setText("");
                tvSeasonalHeadline.setText("Lỗi kết nối. Thử lại sau.");
            }
        });
    }

    private void renderLocalGuideHorizontal(LocalGuideResponse guide) {
        String location = guide.getLocationLabel() != null ? guide.getLocationLabel() : "Vị trí của bạn";
        String season = guide.getSeasonLabel() != null ? guide.getSeasonLabel() : "";
        String locLine = "📍 " + location.toUpperCase(Locale.ROOT);
        if (season != null && !season.isEmpty()) {
            locLine += " - " + season.toUpperCase(Locale.ROOT);
        }
        tvSeasonalLocationLine.setText(locLine);

        containerSuggestionChips.removeAllViews();
        containerSuggestionChips.setVisibility(View.VISIBLE);

        suggestionItems = guide.getSuggestions();
        tvSeasonalHeadline.setText(buildSeasonalHeadline(suggestionItems));
        if (suggestionItems == null || suggestionItems.isEmpty()) return;

        for (int i = 0; i < suggestionItems.size(); i++) {
            LocalGuideResponse.SuggestionItem item = suggestionItems.get(i);
            View chip = createSuggestionChip(item, i);
            containerSuggestionChips.addView(chip);
        }

        // Auto-select first suggestion
        if (!suggestionItems.isEmpty()) {
            selectedSuggestionIndex = 0;
            showSuggestionDetail(suggestionItems.get(0));
            refreshChipSelection();
        }
    }

    private String buildSeasonalHeadline(List<LocalGuideResponse.SuggestionItem> items) {
        if (items == null || items.isEmpty()) {
            return "Khám phá đặc sản địa phương theo mùa 🌿";
        }
        if (items.size() == 1) {
            return items.get(0).getName() + " đang vào mùa — đặc sản địa phương 🌿";
        }
        if (items.size() == 2) {
            return items.get(0).getName() + " & " + items.get(1).getName()
                    + " đang vào mùa — đặc sản địa phương 🌿";
        }
        return items.get(0).getName() + ", " + items.get(1).getName() + " & " + items.get(2).getName()
                + " đang vào mùa — đặc sản địa phương 🌿";
    }

    private View createSuggestionChip(LocalGuideResponse.SuggestionItem item, int index) {
        // Create a compact chip: emoji + name
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), dp(8), dp(14), dp(8));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(Color.parseColor("#F5F5F5"));
        bg.setStroke(dp(1), Color.parseColor("#EEEEEE"));
        chip.setBackground(bg);
        chip.setElevation(0);
        chip.setClickable(true);
        chip.setFocusable(true);

        TextView emojiView = new TextView(this);
        emojiView.setText(item.getEmoji() != null ? item.getEmoji() : "🍽️");
        emojiView.setTextSize(18);
        chip.addView(emojiView);

        TextView nameView = new TextView(this);
        nameView.setText(item.getName());
        nameView.setTextSize(13);
        nameView.setTextColor(Color.parseColor("#212121"));
        LinearLayout.LayoutParams nameLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLP.setMarginStart(dp(6));
        nameView.setLayoutParams(nameLP);
        chip.addView(nameView);

        // Indicator dot for has results
        if (item.isHasResults() && item.getMatchedPosts() != null && !item.getMatchedPosts().isEmpty()) {
            TextView dot = new TextView(this);
            dot.setText(" •");
            dot.setTextColor(Color.parseColor("#1A7A4A"));
            dot.setTextSize(14);
            chip.addView(dot);
        }

        chip.setOnClickListener(v -> {
            if (selectedSuggestionIndex == index) {
                // Toggle off
                cardSuggestionDetail.setVisibility(View.GONE);
                selectedSuggestionIndex = -1;
            } else {
                selectedSuggestionIndex = index;
                showSuggestionDetail(item);
            }
            refreshChipSelection();
        });

        chip.setTag(index);
        return chip;
    }

    private void refreshChipSelection() {
        for (int i = 0; i < containerSuggestionChips.getChildCount(); i++) {
            View chip = containerSuggestionChips.getChildAt(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(20));
            if (i == selectedSuggestionIndex) {
                bg.setColor(Color.parseColor("#E8F5EE"));
                bg.setStroke(dp(2), Color.parseColor("#1A7A4A"));
            } else {
                bg.setColor(Color.parseColor("#F5F5F5"));
                bg.setStroke(dp(1), Color.parseColor("#EEEEEE"));
            }
            chip.setBackground(bg);
            if (chip instanceof LinearLayout && ((LinearLayout) chip).getChildCount() >= 2) {
                View nameChild = ((LinearLayout) chip).getChildAt(1);
                if (nameChild instanceof TextView) {
                    ((TextView) nameChild).setTextColor(
                            i == selectedSuggestionIndex
                                    ? Color.parseColor("#1A7A4A")
                                    : Color.parseColor("#212121"));
                }
            }
        }
    }

    private void showSuggestionDetail(LocalGuideResponse.SuggestionItem item) {
        cardSuggestionDetail.setVisibility(View.VISIBLE);

        tvDetailEmoji.setText(item.getEmoji() != null ? item.getEmoji() : "🍽️");
        tvDetailName.setText(item.getName());
        tvDetailReason.setText(item.getReason());

        listDetailPosts.removeAllViews();

        boolean hasPosts = item.isHasResults() && item.getMatchedPosts() != null && !item.getMatchedPosts().isEmpty();

        if (hasPosts) {
            containerDetailPosts.setVisibility(View.VISIBLE);
            tvDetailNoResults.setVisibility(View.GONE);

            int count = 0;
            for (CommunityPost post : item.getMatchedPosts()) {
                if (count >= 3) break;
                View postView = LayoutInflater.from(this)
                        .inflate(R.layout.item_matched_post_mini, listDetailPosts, false);

                TextView tvTitle = postView.findViewById(R.id.tvPostTitle);
                TextView tvLocation = postView.findViewById(R.id.tvPostLocation);

                tvTitle.setText(post.getTitle());
                String loc = "";
                if (post.getProvince() != null) loc += "📍 " + post.getProvince();
                if (post.getCategoryDisplay() != null) loc += " · " + post.getCategoryDisplay();
                tvLocation.setText(loc);

                final long postId = post.getId();
                postView.setOnClickListener(v -> {
                    Intent intent = new Intent(HomeActivity.this, PostDetailActivity.class);
                    intent.putExtra("POST_ID", postId);
                    startActivity(intent);
                });

                listDetailPosts.addView(postView);
                count++;
            }
        } else {
            containerDetailPosts.setVisibility(View.GONE);
            tvDetailNoResults.setVisibility(View.VISIBLE);
            tvDetailNoResults.setText("Chưa có ai bán gần bạn. Đăng bán ngay?");
        }

        // Action buttons — luôn hiển thị cho cả 2 trường hợp
        String suggestionName = item.getName();

        btnDetailGomChung.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            intent.putExtra("PREFILL_TITLE", "Gom chung mua " + suggestionName);
            intent.putExtra("PREFILL_CATEGORY", "gom_chung");
            startActivity(intent);
        });

        btnDetailNhoMua.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateShoppingRequestActivity.class);
            ArrayList<String> items = new ArrayList<>();
            items.add(suggestionName);
            intent.putStringArrayListExtra("itemNames", items);
            startActivity(intent);
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
