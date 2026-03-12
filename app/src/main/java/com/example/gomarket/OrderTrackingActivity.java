package com.example.gomarket;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.Order;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.receiver.OrderStatusReceiver;
import com.example.gomarket.service.OrderPollingService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderTrackingActivity extends AppCompatActivity
        implements OrderStatusReceiver.OnOrderStatusChangedListener {

    private TextView tvTitle, tvShopperName, tvShopperRating;
    private TextView tvRouteDistance, tvRouteTime, tvRouteShops;
    private TextView stepIcon1, stepIcon2, stepIcon3, stepIcon4;
    private TextView btnChat, btnCall;

    private int orderId;
    private OrderStatusReceiver orderStatusReceiver;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        orderId = getIntent().getIntExtra("order_id", -1);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupClickListeners();

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

        tvTitle.setText("Đơn hàng #" + String.format("%03d", orderId));
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
    }

    private void loadOrderDetails() {
        apiService.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateOrderUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(OrderTrackingActivity.this,
                        "Không thể tải thông tin đơn hàng",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOrderPolling() {
        // Đăng ký BroadcastReceiver
        orderStatusReceiver = new OrderStatusReceiver(this);
        IntentFilter filter = new IntentFilter(OrderPollingService.ACTION_ORDER_STATUS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(orderStatusReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(orderStatusReceiver, filter);
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

        switch (status) {
            case "DELIVERING":
                stepIcon3.setBackgroundResource(R.drawable.bg_icon_green);
                stepIcon3.setText("✓");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderStatusReceiver != null) {
            unregisterReceiver(orderStatusReceiver);
        }
    }
}
