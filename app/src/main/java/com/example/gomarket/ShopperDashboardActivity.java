package com.example.gomarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ShoppingRequestAdapter;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.User;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopperDashboardActivity extends AppCompatActivity
        implements ShoppingRequestAdapter.OnRequestClickListener {

    private RecyclerView rvRequests;
    private ShoppingRequestAdapter adapter;
    private List<ShoppingRequest> requests = new ArrayList<>();
    private LinearLayout emptyState;
    private TextView tvEmptyMessage, tvTodayOrders, tvTotalEarnings, tvRating, tvOnlineStatus;
    private TextView tabNearby, tabMyRequests;
    private SwitchCompat switchOnline;

    private ApiService apiService;
    private SessionManager session;
    private boolean showingNearby = true;
    private double lat = 0, lng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopper_dashboard);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initViews();
        setupRecyclerView();
        getCurrentLocation();
        loadProfile();
    }

    private void initViews() {
        rvRequests = findViewById(R.id.rvRequests);
        emptyState = findViewById(R.id.emptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        tvTodayOrders = findViewById(R.id.tvTodayOrders);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvRating = findViewById(R.id.tvRating);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        switchOnline = findViewById(R.id.switchOnline);
        tabNearby = findViewById(R.id.tabNearby);
        tabMyRequests = findViewById(R.id.tabMyRequests);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabNearby.setOnClickListener(v -> {
            showingNearby = true;
            tabNearby.setBackgroundResource(R.drawable.bg_filter_active);
            tabNearby.setTextColor(getColor(R.color.white));
            tabMyRequests.setBackgroundResource(R.drawable.bg_filter_inactive);
            tabMyRequests.setTextColor(getColor(R.color.text_secondary));
            loadNearbyRequests();
        });

        tabMyRequests.setOnClickListener(v -> {
            showingNearby = false;
            tabMyRequests.setBackgroundResource(R.drawable.bg_filter_active);
            tabMyRequests.setTextColor(getColor(R.color.white));
            tabNearby.setBackgroundResource(R.drawable.bg_filter_inactive);
            tabNearby.setTextColor(getColor(R.color.text_secondary));
            loadMyRequests();
        });

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvOnlineStatus.setText(isChecked ? "Online" : "Offline");
            Map<String, Boolean> body = new HashMap<>();
            body.put("isOnline", isChecked);
            apiService.updateOnlineStatus(session.getUserId(), body).enqueue(new Callback<Map<String, String>>() {
                @Override public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {}
                @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {}
            });
        });
    }

    private void setupRecyclerView() {
        adapter = new ShoppingRequestAdapter(requests, this);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);
    }

    private void loadProfile() {
        apiService.getProfile(session.getUserId()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    switchOnline.setChecked(user.getIsOnline() != null && user.getIsOnline());
                    if (user.getRating() != null) {
                        tvRating.setText(String.format("%.1f", user.getRating()));
                    }
                    if (user.getTotalOrders() != null) {
                        tvTodayOrders.setText(String.valueOf(user.getTotalOrders()));
                    }
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
                // Update location on server
                Map<String, Double> body = new HashMap<>();
                body.put("latitude", lat);
                body.put("longitude", lng);
                apiService.updateUserLocation(session.getUserId(), body).enqueue(new Callback<Map<String, String>>() {
                    @Override public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {}
                    @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {}
                });
            }
            adapter.setShopperLocation(lat, lng);
            loadNearbyRequests();
        });
    }

    private void loadNearbyRequests() {
        if (lat == 0 && lng == 0) {
            lat = 10.7769; lng = 106.7009; // default HCMC
        }
        apiService.getNearbyRequests(lat, lng).enqueue(new Callback<List<ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<ShoppingRequest>> call, Response<List<ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requests.clear();
                    requests.addAll(response.body());
                    adapter.updateRequests(requests);
                    updateEmptyState("Chưa có đơn nào gần bạn");
                } else {
                    updateEmptyState("Không thể tải đơn");
                }
            }
            @Override
            public void onFailure(Call<List<ShoppingRequest>> call, Throwable t) {
                updateEmptyState("Lỗi kết nối");
            }
        });
    }

    private void loadMyRequests() {
        apiService.getShopperRequests(session.getUserId()).enqueue(new Callback<List<ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<ShoppingRequest>> call, Response<List<ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    requests.clear();
                    requests.addAll(response.body());
                    adapter.updateRequests(requests);
                    updateEmptyState("Bạn chưa nhận đơn nào");
                } else {
                    updateEmptyState("Không thể tải đơn");
                }
            }
            @Override
            public void onFailure(Call<List<ShoppingRequest>> call, Throwable t) {
                updateEmptyState("Lỗi kết nối");
            }
        });
    }

    private void updateEmptyState(String message) {
        if (requests.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
            tvEmptyMessage.setText(message);
        } else {
            emptyState.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestClick(ShoppingRequest request) {
        String status = request.getStatus();
        if ("OPEN".equals(status)) {
            // Block self-accept: user cannot accept their own order
            if (request.getUserId() == session.getUserId()) {
                Toast.makeText(this, "Bạn không thể nhận đơn của chính mình",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Build confirm message with fee + distance info
            StringBuilder confirmMsg = new StringBuilder();
            confirmMsg.append("🛒 ").append(request.getItemCount()).append(" món cần mua\n");
            if (request.getBudget() != null) {
                confirmMsg.append("💰 Ngân sách: ").append(String.format("%,.0fđ", request.getBudget())).append("\n");
            }
            if (request.getShopperFee() != null && request.getShopperFee() > 0) {
                confirmMsg.append("🤝 Phí đi chợ bạn nhận được: ").append(String.format("%,.0fđ", request.getShopperFee())).append("\n");
            }
            if (lat != 0 && request.getLatitude() != null) {
                double dist = haversineDistance(lat, lng, request.getLatitude(), request.getLongitude());
                if (dist < 1) {
                    confirmMsg.append("📍 Khoảng cách: ").append((int)(dist * 1000)).append("m\n");
                } else {
                    confirmMsg.append("📍 Khoảng cách: ").append(String.format("%.1f km", dist)).append("\n");
                }
            }
            confirmMsg.append("📍 Giao tại: ").append(request.getDeliveryAddress());

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Nhận đơn #" + String.format("%03d", request.getId()) + "?")
                    .setMessage(confirmMsg.toString())
                    .setPositiveButton("Nhận đơn", (d, w) -> acceptRequest(request))
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            // Already accepted - open shopping screen
            Intent intent = new Intent(this, ShopperShoppingActivity.class);
            intent.putExtra("REQUEST_ID", request.getId());
            startActivity(intent);
        }
    }

    private void acceptRequest(ShoppingRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("shopperId", (long) session.getUserId());
        apiService.acceptRequest(request.getId(), body).enqueue(new Callback<ShoppingRequest>() {
            @Override
            public void onResponse(Call<ShoppingRequest> call, Response<ShoppingRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ShopperDashboardActivity.this,
                            "Đã nhận đơn!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ShopperDashboardActivity.this, ShopperShoppingActivity.class);
                    intent.putExtra("REQUEST_ID", request.getId());
                    startActivity(intent);
                } else {
                    String errorMsg = "Không thể nhận đơn";
                    try {
                        if (response.errorBody() != null) {
                            String err = response.errorBody().string();
                            if (err.contains("chính mình")) {
                                errorMsg = "Bạn không thể nhận đơn của chính mình";
                            } else if (err.contains("đã được nhận")) {
                                errorMsg = "Đơn này đã được người khác nhận";
                            }
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(ShopperDashboardActivity.this,
                            errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ShoppingRequest> call, Throwable t) {
                Toast.makeText(ShopperDashboardActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingNearby) loadNearbyRequests();
        else loadMyRequests();
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}
