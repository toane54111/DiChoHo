package com.example.gomarket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.gomarket.model.Order;
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

    private int orderId;
    private OrderStatusReceiver orderStatusReceiver;
    private ApiService apiService;

    // Map vars
    private MapView map;
    private IMapController mapController;
    private Marker shopperMarker;
    private Marker destinationMarker;
    private double destLat = 10.7769; // Fake default
    private double destLng = 106.7009; // Fake default
    private LocationReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMDroid configuration before loading layout
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_order_tracking);

        orderId = getIntent().getIntExtra("order_id", -1);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupClickListeners();
        setupMap();

        if (orderId > 0) {
            loadOrderDetails();
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

        tvTitle.setText("Đơn hàng #" + String.format("%03d", orderId));
    }

    private void setupMap() {
        map = findViewById(R.id.map);
        map.setMultiTouchControls(false); // Mini-map shouldn't be very interactive
        mapController = map.getController();
        mapController.setZoom(15.0);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        });

        btnCall.setOnClickListener(v ->
                Toast.makeText(this, "Đang gọi cho Shopper...", Toast.LENGTH_SHORT).show()
        );

        btnViewFullMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopperMapActivity.class);
            intent.putExtra("role", "BUYER");
            intent.putExtra("order_id", orderId);
            startActivity(intent);
        });
    }

    private void loadOrderDetails() {
        apiService.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    destLat = order.getLatitude() != 0 ? order.getLatitude() : destLat;
                    destLng = order.getLongitude() != 0 ? order.getLongitude() : destLng;
                    
                    if (map != null) {
                        GeoPoint dest = new GeoPoint(destLat, destLng);
                        mapController.setCenter(dest);
                        
                        destinationMarker = new Marker(map);
                        destinationMarker.setPosition(dest);
                        destinationMarker.setTitle("Nơi giao hàng");
                        // Use default OSMDroid marker or custom drawable
                        map.getOverlays().add(destinationMarker);
                        map.invalidate();
                    }

                    updateOrderUI(order);
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this,
                        "Không thể tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOrderPolling() {
        // Đăng ký Status Receiver
        orderStatusReceiver = new OrderStatusReceiver(this);
        IntentFilter statusFilter = new IntentFilter(OrderPollingService.ACTION_ORDER_STATUS_CHANGED);
        
        // Đăng ký Location Receiver
        locationReceiver = new LocationReceiver();
        IntentFilter locationFilter = new IntentFilter(OrderPollingService.ACTION_SHOPPER_LOCATION_UPDATED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(orderStatusReceiver, statusFilter, RECEIVER_NOT_EXPORTED);
            registerReceiver(locationReceiver, locationFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(orderStatusReceiver, statusFilter);
            registerReceiver(locationReceiver, locationFilter);
        }

        // Start OrderPollingService
        Intent serviceIntent = new Intent(this, OrderPollingService.class);
        serviceIntent.putExtra(OrderPollingService.EXTRA_ORDER_ID, orderId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateOrderUI(Order order) {
        if (order.getShopperName() != null) {
            tvShopperName.setText(order.getShopperName());
        }

        updateStepperUI(order.getStatus());
    }

    private void updateStepperUI(String status) {
        // Reset all steps to gray
        stepIcon3.setBackgroundResource(R.drawable.bg_icon_gray);
        stepIcon4.setBackgroundResource(R.drawable.bg_icon_gray);
        btnViewFullMap.setVisibility(View.GONE);

        switch (status) {
            case "DELIVERING":
                stepIcon3.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon3.setText("✓");
                btnViewFullMap.setVisibility(View.VISIBLE);
                break;
            case "COMPLETED":
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
            // Optionally change marker icon for stale GPS (for OSMDroid, using setIcon with a drawable)
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


