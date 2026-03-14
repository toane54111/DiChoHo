package com.example.gomarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.model.Order;
import com.example.gomarket.model.OrderItem;
import com.example.gomarket.model.OrderRequest;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 200;
    private static final double SHIPPING_FEE = 15000;

    private TextView tvBack, tvSubtotal, tvShippingFee, tvTotal;
    private EditText etAddress;
    private MaterialButton btnGPS, btnConfirm;
    private RadioGroup rgPaymentMethod;
    private LinearLayout layoutWalletInfo;
    private TextView tvWalletBalance;
    private RecyclerView rvItems;

    private double deliveryLat = 0, deliveryLng = 0;
    private ApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private List<OrderItem> cartItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        apiService = ApiClient.getApiService(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        cartItems = CartActivity.getCart();

        initViews();
        updateSummary();
        setupClickListeners();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tvBack);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotal = findViewById(R.id.tvTotal);
        etAddress = findViewById(R.id.etAddress);
        btnGPS = findViewById(R.id.btnGPS);
        btnConfirm = findViewById(R.id.btnConfirm);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        layoutWalletInfo = findViewById(R.id.layoutWalletInfo);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        rvItems = findViewById(R.id.rvCheckoutItems);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(new com.example.gomarket.adapter.CartAdapter(cartItems, null));

        tvShippingFee.setText(String.format("%,.0fđ", SHIPPING_FEE));
    }

    private void updateSummary() {
        double subtotal = 0;
        for (OrderItem item : cartItems) subtotal += item.getSubtotal();
        tvSubtotal.setText(String.format("%,.0fđ", subtotal));
        tvTotal.setText(String.format("%,.0fđ", subtotal + SHIPPING_FEE));
    }

    private void setupClickListeners() {
        tvBack.setOnClickListener(v -> finish());

        btnGPS.setOnClickListener(v -> detectCurrentLocation());

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbWallet) {
                layoutWalletInfo.setVisibility(View.VISIBLE);
                loadWalletBalance();
            } else {
                layoutWalletInfo.setVisibility(View.GONE);
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            if (address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng!", Toast.LENGTH_SHORT).show();
                return;
            }
            placeOrder(address);
        });
    }

    private void detectCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                deliveryLat = location.getLatitude();
                deliveryLng = location.getLongitude();
                etAddress.setText("Vị trí hiện tại (" +
                        String.format("%.4f", deliveryLat) + ", " +
                        String.format("%.4f", deliveryLng) + ")");
                Toast.makeText(this, "Đã lấy vị trí GPS!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể lấy GPS. Hãy nhập địa chỉ thủ công.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWalletBalance() {
        SessionManager session = new SessionManager(this);
        long userId = session.getUserId();
        apiService.getWalletBalance(userId).enqueue(new Callback<com.example.gomarket.model.Wallet>() {
            @Override
            public void onResponse(Call<com.example.gomarket.model.Wallet> call,
                                   Response<com.example.gomarket.model.Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    tvWalletBalance.setText("Số dư: " + String.format("%,dđ", balance));
                }
            }
            @Override
            public void onFailure(Call<com.example.gomarket.model.Wallet> call, Throwable t) {
                tvWalletBalance.setText("Không thể tải số dư ví");
            }
        });
    }

    private void placeOrder(String address) {
        SessionManager session = new SessionManager(this);
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        String paymentMethod = (checkedId == R.id.rbWallet) ? "WALLET" :
                               (checkedId == R.id.rbQR) ? "QR" : "COD";

        List<OrderRequest.OrderItemRequest> items = new ArrayList<>();
        for (OrderItem item : cartItems) {
            items.add(new OrderRequest.OrderItemRequest(item.getProductId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                (int) session.getUserId(),
                address,
                deliveryLat == 0 ? 10.7769 : deliveryLat,
                deliveryLng == 0 ? 106.7009 : deliveryLng,
                items,
                paymentMethod  // ← truyền đúng phương thức thanh toán
        );

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        apiService.createOrder(request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    CartActivity.clearCart();
                    Toast.makeText(CheckoutActivity.this,
                            "Đặt hàng thành công! Đơn #" + order.getId(), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(CheckoutActivity.this, OrderTrackingActivity.class);
                    intent.putExtra("order_id", order.getId());
                    startActivity(intent);
                    finish();
                } else {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Xác nhận đặt hàng");
                    Toast.makeText(CheckoutActivity.this,
                            "Đặt hàng thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Xác nhận đặt hàng");
                Toast.makeText(CheckoutActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectCurrentLocation();
        }
    }
}
