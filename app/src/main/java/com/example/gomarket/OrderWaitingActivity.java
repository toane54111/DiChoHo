package com.example.gomarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.ShoppingRequestItem;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderWaitingActivity extends AppCompatActivity {

    private long requestId;
    private ApiService apiService;
    private SessionManager session;
    private ShoppingRequest currentRequest;
    private Handler pollHandler;
    private Runnable pollRunnable;
    private String lastStatus = "";
    private boolean ratingDialogShown = false;

    // Step views
    private View step1Circle, step2Circle, step3Circle;
    private TextView step1Text, step2Text, step3Text;
    private View line12, line23;

    // Shopper card
    private MaterialCardView cardShopperInfo;
    private TextView tvShopperName, tvShopperPhone, tvShopperRating;
    private MaterialCardView btnCallShopper, btnChatShopper;

    // Status card
    private TextView tvStatusEmoji, tvStatusTitle, tvStatusDesc;
    private ProgressBar progressWaiting;

    // Map
    private MaterialCardView cardMap;
    private MapView mapView;
    private Marker shopperMarker;

    // Order info
    private TextView tvOrderId, tvOrderAddress, tvOrderBudget, tvOrderItems;

    // Cancel
    private MaterialCardView btnCancelOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_waiting);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);
        requestId = getIntent().getLongExtra("REQUEST_ID", -1);

        initViews();
        loadRequest();
        startPolling();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Steps
        step1Circle = findViewById(R.id.step1Circle);
        step2Circle = findViewById(R.id.step2Circle);
        step3Circle = findViewById(R.id.step3Circle);
        step1Text = findViewById(R.id.step1Text);
        step2Text = findViewById(R.id.step2Text);
        step3Text = findViewById(R.id.step3Text);
        line12 = findViewById(R.id.line12);
        line23 = findViewById(R.id.line23);

        // Shopper
        cardShopperInfo = findViewById(R.id.cardShopperInfo);
        tvShopperName = findViewById(R.id.tvShopperName);
        tvShopperPhone = findViewById(R.id.tvShopperPhone);
        tvShopperRating = findViewById(R.id.tvShopperRating);
        btnCallShopper = findViewById(R.id.btnCallShopper);
        btnCallShopper.setOnClickListener(v -> callShopper());
        btnChatShopper = findViewById(R.id.btnChatShopper);
        btnChatShopper.setOnClickListener(v -> openChat());

        // Status
        tvStatusEmoji = findViewById(R.id.tvStatusEmoji);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        progressWaiting = findViewById(R.id.progressWaiting);

        // Map
        cardMap = findViewById(R.id.cardMap);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        shopperMarker = new Marker(mapView);
        shopperMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        shopperMarker.setTitle("Shopper");
        mapView.getOverlays().add(shopperMarker);

        // Order info
        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderAddress = findViewById(R.id.tvOrderAddress);
        tvOrderBudget = findViewById(R.id.tvOrderBudget);
        tvOrderItems = findViewById(R.id.tvOrderItems);

        // Cancel
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnCancelOrder.setOnClickListener(v -> confirmCancelOrder());
    }

    private void loadRequest() {
        if (requestId <= 0) {
            finish();
            return;
        }

        apiService.getShoppingRequest(requestId).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    displayOrderInfo();
                    updateUI();
                }
            }

            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {}
        });
    }

    private void displayOrderInfo() {
        tvOrderId.setText("Đơn #" + String.format("%03d", currentRequest.getId()));

        if (currentRequest.getDeliveryAddress() != null) {
            tvOrderAddress.setText("📍 " + currentRequest.getDeliveryAddress());
        }
        if (currentRequest.getBudget() != null) {
            tvOrderBudget.setText(String.format("💰 Ngân sách: %,.0fđ", currentRequest.getBudget()));
        }

        // Items list
        List<ShoppingRequestItem> items = currentRequest.getItems();
        if (items != null && !items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                ShoppingRequestItem item = items.get(i);
                sb.append("• ").append(item.getItemText());
                if (item.getQuantityNote() != null && !item.getQuantityNote().isEmpty()) {
                    sb.append(" (").append(item.getQuantityNote()).append(")");
                }
                if (i < items.size() - 1) sb.append("\n");
            }
            tvOrderItems.setText(sb.toString());
        }
    }

    private void updateUI() {
        if (currentRequest == null) return;
        String status = currentRequest.getStatus();
        if (status == null) status = "OPEN";

        // Avoid redundant updates
        if (status.equals(lastStatus)) return;
        lastStatus = status;

        // Update toolbar
        TextView tvToolbar = findViewById(R.id.tvToolbarTitle);

        // Cancel button only visible when OPEN
        btnCancelOrder.setVisibility("OPEN".equals(status) ? View.VISIBLE : View.GONE);

        switch (status) {
            case "OPEN":
                setStep(1);
                tvStatusEmoji.setText("🔍");
                tvStatusTitle.setText("Đang tìm người đi chợ hộ...");
                tvStatusDesc.setText("Đơn hàng của bạn đang được gửi đến các shopper gần đây.\nVui lòng chờ trong giây lát...");
                progressWaiting.setVisibility(View.VISIBLE);
                cardShopperInfo.setVisibility(View.GONE);
                cardMap.setVisibility(View.GONE);
                tvToolbar.setText("Đang tìm shopper...");
                break;

            case "ACCEPTED":
                setStep(1);
                tvStatusEmoji.setText("✅");
                tvStatusTitle.setText("Đã có người nhận đơn!");
                tvStatusDesc.setText("Shopper đang chuẩn bị đi chợ cho bạn.\nHọ sẽ bắt đầu mua sắm ngay thôi!");
                progressWaiting.setVisibility(View.GONE);
                showShopperInfo();
                cardMap.setVisibility(View.GONE);
                tvToolbar.setText("Đã tìm được shopper");
                break;

            case "SHOPPING":
                setStep(2);
                tvStatusEmoji.setText("🛒");
                tvStatusTitle.setText("Đang đi chợ cho bạn...");
                tvStatusDesc.setText("Shopper đang mua sắm theo danh sách của bạn.\nBạn sẽ nhận được thông báo khi hoàn tất!");
                progressWaiting.setVisibility(View.VISIBLE);
                showShopperInfo();
                cardMap.setVisibility(View.GONE);
                tvToolbar.setText("Đang đi chợ");
                break;

            case "DELIVERING":
                setStep(3);
                tvStatusEmoji.setText("🚗");
                tvStatusTitle.setText("Đang giao hàng đến bạn!");
                tvStatusDesc.setText("Shopper đã mua xong và đang trên đường giao hàng.\nChuẩn bị nhận hàng nhé!");
                progressWaiting.setVisibility(View.GONE);
                showShopperInfo();
                showMap();
                tvToolbar.setText("Đang giao hàng");
                break;

            case "COMPLETED":
                setStep(3);
                activateStep(step1Circle, step1Text);
                activateStep(step2Circle, step2Text);
                activateStep(step3Circle, step3Text);
                line12.setBackgroundColor(getColor(R.color.primary));
                line23.setBackgroundColor(getColor(R.color.primary));
                tvStatusEmoji.setText("🎉");
                tvStatusTitle.setText("Giao hàng thành công!");
                tvStatusDesc.setText("Đơn hàng đã được giao thành công.\nCảm ơn bạn đã sử dụng GoMarket!");
                progressWaiting.setVisibility(View.GONE);
                showShopperInfo();
                cardMap.setVisibility(View.GONE);
                tvToolbar.setText("Hoàn thành");
                stopPolling();

                // Show rating dialog after 2s debounce
                if (!ratingDialogShown) {
                    ratingDialogShown = true;
                    new Handler(Looper.getMainLooper()).postDelayed(this::showRatingDialog, 2000);
                }
                break;

            case "CANCELLED":
                tvStatusEmoji.setText("❌");
                tvStatusTitle.setText("Đơn hàng đã bị hủy");
                tvStatusDesc.setText("");
                progressWaiting.setVisibility(View.GONE);
                tvToolbar.setText("Đã hủy");
                stopPolling();
                break;
        }
    }

    // ═══ RATING ═══

    private void showRatingDialog() {
        if (currentRequest == null || currentRequest.getShopperId() == null) return;

        // Check if already reviewed
        apiService.getReviewForRequest(requestId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object reviewed = response.body().get("reviewed");
                    if (reviewed != null && Boolean.FALSE.equals(reviewed)) {
                        // Not reviewed yet, show dialog
                        displayRatingDialog();
                    }
                    // If it has an "id" field, it means already reviewed
                    if (response.body().containsKey("id")) {
                        // Already reviewed
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void displayRatingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rate_shopper, null);

        TextView tvName = dialogView.findViewById(R.id.tvRateShopperName);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        TextView tvLabel = dialogView.findViewById(R.id.tvRatingLabel);
        EditText etComment = dialogView.findViewById(R.id.etReviewComment);

        tvName.setText(currentRequest.getShopperName() != null ?
                currentRequest.getShopperName() : "Shopper");

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            int r = (int) rating;
            switch (r) {
                case 1: tvLabel.setText("Rất tệ"); tvLabel.setTextColor(0xFFF44336); break;
                case 2: tvLabel.setText("Tệ"); tvLabel.setTextColor(0xFFFF9800); break;
                case 3: tvLabel.setText("Bình thường"); tvLabel.setTextColor(0xFFFFC107); break;
                case 4: tvLabel.setText("Tốt"); tvLabel.setTextColor(0xFF8BC34A); break;
                case 5: tvLabel.setText("Tuyệt vời!"); tvLabel.setTextColor(0xFF4CAF50); break;
            }
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Gửi đánh giá", (d, w) -> {
                    int rating = (int) ratingBar.getRating();
                    String comment = etComment.getText().toString().trim();
                    submitReview(rating, comment);
                })
                .setNegativeButton("Bỏ qua", null)
                .setCancelable(false)
                .show();
    }

    private void submitReview(int rating, String comment) {
        if (currentRequest == null || currentRequest.getShopperId() == null) return;

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", requestId);
        body.put("buyerId", (long) session.getUserId());
        body.put("shopperId", currentRequest.getShopperId());
        body.put("rating", rating);
        body.put("comment", comment);

        apiService.createReview(body).enqueue(new Callback<com.example.gomarket.model.ShopperReview>() {
            @Override
            public void onResponse(Call<com.example.gomarket.model.ShopperReview> call,
                                   Response<com.example.gomarket.model.ShopperReview> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderWaitingActivity.this,
                            "Cảm ơn bạn đã đánh giá! ⭐", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OrderWaitingActivity.this,
                            "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.gomarket.model.ShopperReview> call, Throwable t) {
                Toast.makeText(OrderWaitingActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ═══ SHOPPER INFO + RATING ═══

    private void setStep(int activeStep) {
        deactivateStep(step1Circle, step1Text);
        deactivateStep(step2Circle, step2Text);
        deactivateStep(step3Circle, step3Text);
        line12.setBackgroundColor(0xFFE0E0E0);
        line23.setBackgroundColor(0xFFE0E0E0);

        if (activeStep >= 1) activateStep(step1Circle, step1Text);
        if (activeStep >= 2) {
            activateStep(step2Circle, step2Text);
            line12.setBackgroundColor(getColor(R.color.primary));
        }
        if (activeStep >= 3) {
            activateStep(step3Circle, step3Text);
            line23.setBackgroundColor(getColor(R.color.primary));
        }
    }

    private void activateStep(View circle, TextView text) {
        circle.setBackgroundResource(R.drawable.bg_badge_ai);
        text.setTextColor(getColor(R.color.primary));
        text.setTextSize(11);
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
    }

    private void deactivateStep(View circle, TextView text) {
        circle.setBackgroundResource(R.drawable.bg_input_rounded);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTypeface(text.getTypeface(), android.graphics.Typeface.NORMAL);
    }

    private void showShopperInfo() {
        if (currentRequest.getShopperName() != null) {
            cardShopperInfo.setVisibility(View.VISIBLE);
            tvShopperName.setText(currentRequest.getShopperName());
            if (currentRequest.getShopperPhone() != null) {
                tvShopperPhone.setText("📱 " + currentRequest.getShopperPhone());
                btnCallShopper.setVisibility(View.VISIBLE);
            }
            btnChatShopper.setVisibility(View.VISIBLE);

            // Load shopper rating
            if (currentRequest.getShopperId() != null) {
                loadShopperRating(currentRequest.getShopperId());
            }
        }
    }

    private void loadShopperRating(long shopperId) {
        apiService.getShopperRatingSummary(shopperId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object avgObj = response.body().get("averageRating");
                    Object countObj = response.body().get("totalReviews");
                    double avg = avgObj instanceof Number ? ((Number) avgObj).doubleValue() : 5.0;
                    long count = countObj instanceof Number ? ((Number) countObj).longValue() : 0;

                    if (tvShopperRating != null) {
                        tvShopperRating.setVisibility(View.VISIBLE);
                        tvShopperRating.setText(String.format("⭐ %.1f (%d)", avg, count));
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    // ═══ CHAT ═══

    private void openChat() {
        if (currentRequest == null || currentRequest.getShopperId() == null) return;

        Intent intent = new Intent(this, OrderChatActivity.class);
        intent.putExtra("REQUEST_ID", requestId);
        intent.putExtra("OTHER_USER_ID", currentRequest.getShopperId().longValue());
        intent.putExtra("OTHER_USER_NAME", currentRequest.getShopperName());
        startActivity(intent);
    }

    private void showMap() {
        cardMap.setVisibility(View.VISIBLE);

        Double lat = currentRequest.getLatitude();
        Double lng = currentRequest.getLongitude();
        if (lat != null && lng != null) {
            GeoPoint deliveryPoint = new GeoPoint(lat, lng);
            mapView.getController().setCenter(deliveryPoint);
            mapView.getController().setZoom(15.0);

            Marker deliveryMarker = new Marker(mapView);
            deliveryMarker.setPosition(deliveryPoint);
            deliveryMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            deliveryMarker.setTitle("Điểm giao hàng");
            mapView.getOverlays().add(deliveryMarker);
        }

        Double shopperLat = currentRequest.getShopperLat();
        Double shopperLng = currentRequest.getShopperLng();
        if (shopperLat != null && shopperLng != null) {
            GeoPoint shopperPoint = new GeoPoint(shopperLat, shopperLng);
            shopperMarker.setPosition(shopperPoint);
            shopperMarker.setTitle("🛵 " + (currentRequest.getShopperName() != null ?
                    currentRequest.getShopperName() : "Shopper"));
            mapView.invalidate();
        }
    }

    private void callShopper() {
        if (currentRequest != null && currentRequest.getShopperPhone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + currentRequest.getShopperPhone()));
            startActivity(intent);
        }
    }

    // ═══ CANCEL ORDER ═══

    private void confirmCancelOrder() {
        if (currentRequest == null) return;
        if (!"OPEN".equals(currentRequest.getStatus())) {
            Toast.makeText(this, "Không thể hủy đơn đã được nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        String msg = "Bạn có chắc muốn hủy đơn #" + String.format("%03d", currentRequest.getId()) + "?";
        if ("WALLET".equals(currentRequest.getPaymentMethod())
                && currentRequest.getFrozenAmount() != null && currentRequest.getFrozenAmount() > 0) {
            msg += "\n\n💰 Số tiền " + String.format("%,.0fđ", currentRequest.getFrozenAmount())
                    + " đã đóng băng sẽ được hoàn lại vào ví.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng?")
                .setMessage(msg)
                .setPositiveButton("Hủy đơn", (d, w) -> cancelOrder())
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder() {
        btnCancelOrder.setEnabled(false);

        apiService.cancelRequest(requestId).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    lastStatus = ""; // force UI update
                    updateUI();
                    Toast.makeText(OrderWaitingActivity.this,
                            "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                } else {
                    btnCancelOrder.setEnabled(true);
                    String errorMsg = "Không thể hủy đơn";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(OrderWaitingActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                btnCancelOrder.setEnabled(true);
                Toast.makeText(OrderWaitingActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ═══ POLLING ═══

    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                pollStatus();
                pollHandler.postDelayed(this, 5000);
            }
        };
        pollHandler.postDelayed(pollRunnable, 5000);
    }

    private void stopPolling() {
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }

    private void pollStatus() {
        if (requestId <= 0) return;

        apiService.getShoppingRequest(requestId).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentRequest = response.body();
                    updateUI();

                    if ("DELIVERING".equals(currentRequest.getStatus())) {
                        showMap();
                    }
                }
            }

            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        lastStatus = "";
        loadRequest();
        if (pollHandler != null && pollRunnable != null) {
            pollHandler.postDelayed(pollRunnable, 5000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}
