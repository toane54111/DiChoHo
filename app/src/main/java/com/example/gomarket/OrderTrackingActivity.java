package com.example.gomarket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.ShoppingRequestItem;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.receiver.OrderStatusReceiver;
import com.example.gomarket.service.OrderPollingService;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderTrackingActivity extends AppCompatActivity
        implements OrderStatusReceiver.OnOrderStatusChangedListener {

    private TextView tvTitle, tvShopperName, tvShopperRating;
    private TextView tvRouteDistance, tvRouteTime, tvRouteShops, tvGpsStatus;
    private TextView stepIcon1, stepIcon2, stepIcon3, stepIcon4;
    private TextView btnChat, btnCall;
    private MaterialButton btnViewFullMap;

    private long requestId;
    private String shopperPhone;
    private OrderStatusReceiver orderStatusReceiver;
    private ApiService apiService;

    // Map vars
    private MapView map;
    private IMapController mapController;
    private Marker shopperMarker;
    private Marker destinationMarker;
    private double destLat = 10.7769;
    private double destLng = 106.7009;
    private LocationReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_order_tracking);

        requestId = getIntent().getLongExtra("REQUEST_ID", -1);
        // Fallback for legacy int extra
        if (requestId <= 0) {
            requestId = getIntent().getIntExtra("order_id", -1);
        }
        apiService = ApiClient.getApiService(this);

        initViews();
        setupClickListeners();
        setupMap();

        if (requestId > 0) {
            loadRequestDetails();
            startOrderPolling();
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvShopperName = findViewById(R.id.tvShopperName);
        tvShopperRating = findViewById(R.id.tvShopperRating);
        tvRouteDistance = findViewById(R.id.tvRouteDistance);
        tvRouteTime = findViewById(R.id.tvRouteTime);
        tvRouteShops = findViewById(R.id.tvRouteShops);
        stepIcon1 = findViewById(R.id.stepIcon1);
        stepIcon2 = findViewById(R.id.stepIcon2);
        stepIcon3 = findViewById(R.id.stepIcon3);
        stepIcon4 = findViewById(R.id.stepIcon4);
        btnChat = findViewById(R.id.btnChat);
        btnCall = findViewById(R.id.btnCall);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        btnViewFullMap = findViewById(R.id.btnViewFullMap);

        tvTitle.setText("Đơn #" + String.format("%03d", requestId));
    }

    private void setupMap() {
        map = findViewById(R.id.map);
        map.setMultiTouchControls(false);
        mapController = map.getController();
        mapController.setZoom(15.0);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        });

        btnCall.setOnClickListener(v -> {
            if (shopperPhone != null && !shopperPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + shopperPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Chưa có thông tin shopper", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewFullMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopperMapActivity.class);
            intent.putExtra("role", "BUYER");
            intent.putExtra("REQUEST_ID", requestId);
            startActivity(intent);
        });
    }

    private void loadRequestDetails() {
        apiService.getShoppingRequest(requestId).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShoppingRequest req = response.body();
                    if (req.getLatitude() != null && req.getLatitude() != 0) {
                        destLat = req.getLatitude();
                    }
                    if (req.getLongitude() != null && req.getLongitude() != 0) {
                        destLng = req.getLongitude();
                    }

                    if (map != null) {
                        GeoPoint dest = new GeoPoint(destLat, destLng);
                        mapController.setCenter(dest);

                        destinationMarker = new Marker(map);
                        destinationMarker.setPosition(dest);
                        destinationMarker.setTitle("Nơi giao hàng");
                        map.getOverlays().add(destinationMarker);
                        map.invalidate();
                    }

                    updateRequestUI(req);
                }
            }

            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this,
                        "Không thể tải thông tin đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRequestUI(ShoppingRequest req) {
        if (req.getShopperName() != null) {
            tvShopperName.setText(req.getShopperName());
        } else {
            tvShopperName.setText("Đang tìm shopper...");
        }

        shopperPhone = req.getShopperPhone();

        // Show items count in route info
        int itemCount = req.getItems() != null ? req.getItems().size() : 0;
        tvRouteShops.setText(itemCount + " món");

        // Budget in distance field
        if (req.getBudget() != null) {
            tvRouteDistance.setText(String.format("%,.0fđ", req.getBudget()));
        }

        updateStepperUI(req.getStatus());
    }

    private void startOrderPolling() {
        orderStatusReceiver = new OrderStatusReceiver(this);
        IntentFilter statusFilter = new IntentFilter(OrderPollingService.ACTION_ORDER_STATUS_CHANGED);

        locationReceiver = new LocationReceiver();
        IntentFilter locationFilter = new IntentFilter(OrderPollingService.ACTION_SHOPPER_LOCATION_UPDATED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(orderStatusReceiver, statusFilter, RECEIVER_NOT_EXPORTED);
            registerReceiver(locationReceiver, locationFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(orderStatusReceiver, statusFilter);
            registerReceiver(locationReceiver, locationFilter);
        }

        Intent serviceIntent = new Intent(this, OrderPollingService.class);
        serviceIntent.putExtra(OrderPollingService.EXTRA_ORDER_ID, (int) requestId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateStepperUI(String status) {
        if (status == null) return;

        stepIcon1.setBackgroundResource(R.drawable.bg_icon_green);
        stepIcon1.setText("✓");

        stepIcon2.setBackgroundResource(R.drawable.bg_icon_gray);
        stepIcon3.setBackgroundResource(R.drawable.bg_icon_gray);
        stepIcon4.setBackgroundResource(R.drawable.bg_icon_gray);
        btnViewFullMap.setVisibility(View.GONE);

        switch (status) {
            case "ACCEPTED":
                stepIcon2.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon2.setText("✓");
                break;
            case "SHOPPING":
                stepIcon2.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon2.setText("✓");
                stepIcon3.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon3.setText("✓");
                break;
            case "DELIVERING":
                stepIcon2.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon2.setText("✓");
                stepIcon3.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon3.setText("✓");
                btnViewFullMap.setVisibility(View.VISIBLE);
                break;
            case "COMPLETED":
                stepIcon2.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon2.setText("✓");
                stepIcon3.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon3.setText("✓");
                stepIcon4.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon4.setText("✓");
                break;
        }
    }

    @Override
    public void onOrderStatusChanged(int orderId, String status, String statusText) {
        runOnUiThread(() -> {
            updateStepperUI(status);
            Toast.makeText(this, statusText, Toast.LENGTH_SHORT).show();
        });
    }

    public void updateMiniMap(double lat, double lng, boolean isStale) {
        if (map == null) return;
        GeoPoint shopperPos = new GeoPoint(lat, lng);

        if (shopperMarker == null) {
            shopperMarker = new Marker(map);
            shopperMarker.setTitle("Shopper");
            map.getOverlays().add(shopperMarker);
        }

        shopperMarker.setPosition(shopperPos);
        mapController.animateTo(shopperPos);

        if (isStale) {
            tvGpsStatus.setVisibility(View.VISIBLE);
        } else {
            tvGpsStatus.setVisibility(View.GONE);
        }
        map.invalidate();
    }

    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (OrderPollingService.ACTION_SHOPPER_LOCATION_UPDATED.equals(intent.getAction())) {
                double lat = intent.getDoubleExtra(OrderPollingService.EXTRA_SHOPPER_LAT, 0);
                double lng = intent.getDoubleExtra(OrderPollingService.EXTRA_SHOPPER_LNG, 0);
                boolean isStale = intent.getBooleanExtra(OrderPollingService.EXTRA_SHOPPER_STALE, false);
                updateMiniMap(lat, lng, isStale);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (requestId > 0) loadRequestDetails();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderStatusReceiver != null) unregisterReceiver(orderStatusReceiver);
        if (locationReceiver != null) unregisterReceiver(locationReceiver);
    }
}
