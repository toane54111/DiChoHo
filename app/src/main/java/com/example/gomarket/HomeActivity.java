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
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements PostFeedAdapter.OnPostActionListener {

    private MaterialCardView btnNotification, searchBar;
    private MaterialCardView btnWallet;
    private TextView tvGreeting, tvAddress, tvWalletBalance;

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
    private TextView tvLocalGuideSubtitle;
    private MaterialCardView cardSuggestionDetail;
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
        tvAddress = findViewById(R.id.tvAddress);
        btnWallet = findViewById(R.id.btnWallet);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        btnNotification = findViewById(R.id.btnNotification);
        searchBar = findViewById(R.id.searchBar);

        // AI Thổ Địa
        containerSuggestionChips = findViewById(R.id.containerSuggestionChips);
        localGuideLoading = findViewById(R.id.localGuideLoading);
        tvLocalGuideSubtitle = findViewById(R.id.tvLocalGuideSubtitle);
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
        String name = session.getUserName();
        if (name != null && !name.isEmpty() && !name.equals("Người dùng")) {
            tvGreeting.setText("Chào, " + name + "! 👋");
        } else {
            tvGreeting.setText("Chào bạn! 👋");
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
                    tvLocalGuideSubtitle.setText("Không thể tải gợi ý. Thử lại sau.");
                }
            }
            @Override
            public void onFailure(Call<LocalGuideResponse> call, Throwable t) {
                localGuideLoading.setVisibility(View.GONE);
                tvLocalGuideSubtitle.setText("Lỗi kết nối. Thử lại sau.");
            }
        });
    }

    private void renderLocalGuideHorizontal(LocalGuideResponse guide) {
        String location = guide.getLocationLabel() != null ? guide.getLocationLabel() : "Vị trí của bạn";
        // Rút gọn tên thành phố dài
        location = location.replace("Thành phố Hồ Chí Minh", "TP. Hồ Chí Minh")
                           .replace("Thành Phố Hồ Chí Minh", "TP. Hồ Chí Minh")
                           .replace("Thành phố Hà Nội", "TP. Hà Nội")
                           .replace("Thành phố Đà Nẵng", "TP. Đà Nẵng")
                           .replace("Thành phố Cần Thơ", "TP. Cần Thơ")
                           .replace("Thành phố Hải Phòng", "TP. Hải Phòng");
        String season = guide.getSeasonLabel() != null ? guide.getSeasonLabel() : "";
        tvLocalGuideSubtitle.setText("📍 " + location + " — " + season);

        containerSuggestionChips.removeAllViews();
        containerSuggestionChips.setVisibility(View.VISIBLE);

        suggestionItems = guide.getSuggestions();
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
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), Color.parseColor("#E0E0E0"));
        chip.setBackground(bg);
        chip.setElevation(dp(2));
        chip.setClickable(true);
        chip.setFocusable(true);

        TextView emojiView = new TextView(this);
        emojiView.setText(resolveEmoji(item.getEmoji(), item.getName()));
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
            dot.setTextColor(Color.parseColor("#4CAF50"));
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
                bg.setColor(Color.parseColor("#FFF8E1"));
                bg.setStroke(dp(2), Color.parseColor("#FF9800"));
            } else {
                bg.setColor(Color.WHITE);
                bg.setStroke(dp(1), Color.parseColor("#E0E0E0"));
            }
            chip.setBackground(bg);
        }
    }

    private void showSuggestionDetail(LocalGuideResponse.SuggestionItem item) {
        cardSuggestionDetail.setVisibility(View.VISIBLE);

        tvDetailEmoji.setText(resolveEmoji(item.getEmoji(), item.getName()));
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
            tvDetailNoResults.setText("Chưa có ai bán " + item.getName() + " quanh bạn. Bạn có đang thèm không?");
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

    /**
     * Kiểm tra emoji từ Gemini có hợp lệ không, nếu không thì tra từ tên sản phẩm.
     * Gemini đôi khi trả text như "Durian" thay vì emoji thật.
     */
    private String resolveEmoji(String emoji, String name) {
        // Kiểm tra emoji hợp lệ: emoji thật thường có ≤ 2 ký tự (hoặc codepoint > 0x1F000)
        if (emoji != null && !emoji.isEmpty() && emoji.length() <= 2) {
            int cp = emoji.codePointAt(0);
            if (cp > 0x1F000 || cp > 0x2600) return emoji; // Unicode emoji range
        }

        // Fallback: tra theo tên sản phẩm
        if (name == null) return "🍽️";
        String lower = name.toLowerCase();

        if (lower.contains("sầu riêng") || lower.contains("durian")) return "🥑";
        if (lower.contains("xoài") || lower.contains("mango")) return "🥭";
        if (lower.contains("bưởi") || lower.contains("pomelo")) return "🍊";
        if (lower.contains("cam")) return "🍊";
        if (lower.contains("vải") || lower.contains("lychee")) return "🍒";
        if (lower.contains("nhãn") || lower.contains("longan")) return "🫐";
        if (lower.contains("dừa") || lower.contains("coconut")) return "🥥";
        if (lower.contains("cà phê") || lower.contains("coffee")) return "☕";
        if (lower.contains("chè") || lower.contains("trà") || lower.contains("tea")) return "🍵";
        if (lower.contains("gạo") || lower.contains("cơm") || lower.contains("rice")) return "🍚";
        if (lower.contains("phở") || lower.contains("bún") || lower.contains("mì")) return "🍜";
        if (lower.contains("bánh")) return "🥮";
        if (lower.contains("tôm") || lower.contains("shrimp")) return "🦐";
        if (lower.contains("cua") || lower.contains("crab")) return "🦀";
        if (lower.contains("cá") || lower.contains("fish")) return "🐟";
        if (lower.contains("hải sản") || lower.contains("seafood")) return "🦞";
        if (lower.contains("gà") || lower.contains("chicken")) return "🍗";
        if (lower.contains("bò") || lower.contains("beef")) return "🥩";
        if (lower.contains("heo") || lower.contains("thịt")) return "🥩";
        if (lower.contains("rau") || lower.contains("vegetable")) return "🥬";
        if (lower.contains("ớt") || lower.contains("chili")) return "🌶️";
        if (lower.contains("dâu") || lower.contains("strawberry")) return "🍓";
        if (lower.contains("nho") || lower.contains("grape")) return "🍇";
        if (lower.contains("thanh long") || lower.contains("dragon")) return "🐉";
        if (lower.contains("măng cụt") || lower.contains("mangosteen")) return "🟣";
        if (lower.contains("chôm chôm") || lower.contains("rambutan")) return "🔴";
        if (lower.contains("mắm") || lower.contains("nước mắm")) return "🫙";
        if (lower.contains("khoai")) return "🍠";
        if (lower.contains("bơ") || lower.contains("avocado")) return "🥑";
        if (lower.contains("nem") || lower.contains("chả giò")) return "🥟";
        if (lower.contains("lẩu") || lower.contains("hotpot")) return "🍲";
        if (lower.contains("yến")) return "🪺";
        if (lower.contains("mít") || lower.contains("jackfruit")) return "🍈";

        return "🍽️";
    }
}
