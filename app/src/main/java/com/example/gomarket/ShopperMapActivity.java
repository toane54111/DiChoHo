package com.example.gomarket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.gomarket.model.Order;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.service.LocationService;
import com.example.gomarket.service.OrderPollingService;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopperMapActivity extends AppCompatActivity {

    private String role = "BUYER"; // "SHOPPER" or "BUYER"
    private int orderId = -1;
    
    // UI
    private TextView tvGpsStatus;
    private Button btnChonShopper;
    private View btnCall, btnChat;
    
    // Map
    private MapView map;
    private IMapController mapController;
    private Marker shopperMarker;
    private Marker destinationMarker;
    
    private ApiService apiService;
    private LocationReceiver locationReceiver;
    
    // Status
    private boolean isDelivering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMDroid configuration before loading layout
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_shopper_map);

        role = getIntent().getStringExtra("role");
        if (role == null) role = "BUYER";
        
        orderId = getIntent().getIntExtra("order_id", -1);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupMap();
        
        if (orderId > 0) {
            setupRoleLogic();
        }
    }

    private void initViews() {
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        btnChonShopper = findViewById(R.id.btnChonShopper);
        btnCall = findViewById(R.id.btnCall);
        btnChat = findViewById(R.id.btnChat);

        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang gọi...", Toast.LENGTH_SHORT).show());
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
    }

    private void setupMap() {
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(15.0);
        
        // Load điểm giao hàng
        if (orderId > 0) {
            apiService.getOrder(orderId).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Order order = response.body();
                        double lat = order.getLatitude() != 0 ? order.getLatitude() : 10.7769;
                        double lng = order.getLongitude() != 0 ? order.getLongitude() : 106.7009;
                        
                        GeoPoint dest = new GeoPoint(lat, lng);
                        mapController.setCenter(dest);
                        
                        destinationMarker = new Marker(map);
                        destinationMarker.setPosition(dest);
                        destinationMarker.setTitle("Nơi giao hàng");
                        map.getOverlays().add(destinationMarker);
                        map.invalidate();
                    }
                }
                @Override
                public void onFailure(Call<Order> call, Throwable t) {}
            });
        }
    }

    private void setupRoleLogic() {
        if ("SHOPPER".equals(role)) {
            // Shopper role
            btnChonShopper.setText("Bắt đầu giao hàng");
            btnChonShopper.setOnClickListener(v -> {
                if (!isDelivering) {
                    startDelivering();
                } else {
                    finishDelivering();
                }
            });
        } else {
            // Buyer role
            btnChonShopper.setVisibility(View.GONE); // Buyer chỉ xem
            
            // Đăng ký nhận vị trí từ polling service
            locationReceiver = new LocationReceiver();
            IntentFilter filter = new IntentFilter(OrderPollingService.ACTION_SHOPPER_LOCATION_UPDATED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(locationReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(locationReceiver, filter);
            }
        }
    }

    private void startDelivering() {
        // Cập nhật API -> DELIVERING
        apiService.updateOrderStatus(orderId, Map.of("status", "DELIVERING")).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                isDelivering = true;
                btnChonShopper.setText("Hoàn thành đơn hàng");
                btnChonShopper.setBackgroundColor(0xFF4CAF50); // Màu xanh
                
                // Start LocationService để push vị trí
                Intent serviceIntent = new Intent(ShopperMapActivity.this, LocationService.class);
                serviceIntent.putExtra("delivery_order_id", orderId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(ShopperMapActivity.this, "Đã bắt đầu giao dịch", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<Order> call, Throwable t) {}
        });
    }

    private void finishDelivering() {
        // Cập nhật API -> COMPLETED
        apiService.updateOrderStatus(orderId, Map.of("status", "COMPLETED")).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                // Stop service
                stopService(new Intent(ShopperMapActivity.this, LocationService.class));
                Toast.makeText(ShopperMapActivity.this, "Giao hàng thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<Order> call, Throwable t) {}
        });
    }

    private void updateShopperMarker(double lat, double lng, boolean isStale) {
        if (map == null) return;
        
        GeoPoint pos = new GeoPoint(lat, lng);
        if (shopperMarker == null) {
            shopperMarker = new Marker(map);
            shopperMarker.setTitle("Shopper");
            map.getOverlays().add(shopperMarker);
            mapController.animateTo(pos);
        }
        
        shopperMarker.setPosition(pos);

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
                updateShopperMarker(lat, lng, isStale);
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
        if (locationReceiver != null) {
            unregisterReceiver(locationReceiver);
        }
    }
}
