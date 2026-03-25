package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.Order;
import com.example.gomarket.model.User;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout btnPersonalInfo, btnAddress, btnWallet, btnOrderHistory, btnSettings, btnHelp;
    private MaterialButton btnLogout;
    private TextView tvName, tvPhone, tvEmail, tvRole, tvRoleIcon;
    private TextView tvStatOrders, tvStatBalance, tvStatCompleted;
    private LinearLayout statWallet;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupUserInfo();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileFromServer();
        loadStats();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        tvRoleIcon = findViewById(R.id.tvRoleIcon);
        tvStatOrders = findViewById(R.id.tvStatOrders);
        tvStatBalance = findViewById(R.id.tvStatBalance);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);
        statWallet = findViewById(R.id.statWallet);
        btnPersonalInfo = findViewById(R.id.btnPersonalInfo);
        btnAddress = findViewById(R.id.btnAddress);
        btnWallet = findViewById(R.id.btnWallet);
        btnOrderHistory = findViewById(R.id.btnOrderHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHelp = findViewById(R.id.btnHelp);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupUserInfo() {
        String userName = sessionManager.getUserName();
        if (!userName.isEmpty()) {
            tvName.setText(userName);
        }

        String phone = sessionManager.getUserPhone();
        if (!phone.isEmpty()) {
            tvPhone.setText(phone);
        }

        // Hiển thị email
        String email = sessionManager.getUserEmail();
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }

        // Hiển thị badge vai trò
        String role = sessionManager.getUserRole();
        if ("SHOPPER".equals(role)) {
            tvRoleIcon.setText("🛍️");
            tvRole.setText("Người đi chợ");
        } else {
            tvRoleIcon.setText("🛒");
            tvRole.setText("Người mua");
        }
    }

    private void loadProfileFromServer() {
        long userId = sessionManager.getUserId();
        if (userId <= 0) return;

        apiService.getProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    // Cập nhật session với dữ liệu mới nhất từ server
                    sessionManager.saveLogin(
                            sessionManager.getToken(),
                            user.getId(),
                            user.getFullName(),
                            user.getPhone(),
                            user.getEmail(),
                            user.getRole()
                    );
                    // Cập nhật UI
                    setupUserInfo();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Giữ dữ liệu local nếu không kết nối được server
            }
        });
    }

    private void loadStats() {
        long userId = sessionManager.getUserId();

        // Load số đơn đi chợ hộ
        apiService.getUserShoppingRequests(userId).enqueue(new Callback<List<com.example.gomarket.model.ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<com.example.gomarket.model.ShoppingRequest>> call,
                                   Response<List<com.example.gomarket.model.ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    var requests = response.body();
                    tvStatOrders.setText(String.valueOf(requests.size()));

                    int completed = 0;
                    for (var req : requests) {
                        if ("COMPLETED".equals(req.getStatus())) completed++;
                    }
                    tvStatCompleted.setText(String.valueOf(completed));
                } else {
                    tvStatOrders.setText("0");
                    tvStatCompleted.setText("0");
                }
            }

            @Override
            public void onFailure(Call<List<com.example.gomarket.model.ShoppingRequest>> call, Throwable t) {
                tvStatOrders.setText("0");
                tvStatCompleted.setText("0");
            }
        });

        // Load số dư ví
        apiService.getWalletBalance(userId).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    if (balance >= 1_000_000) {
                        tvStatBalance.setText(String.format("%.1fM", balance / 1_000_000.0));
                    } else if (balance >= 1_000) {
                        tvStatBalance.setText(String.format("%dK", balance / 1_000));
                    } else {
                        tvStatBalance.setText(String.format("%,d", balance));
                    }
                } else {
                    tvStatBalance.setText("0");
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvStatBalance.setText("0");
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPersonalInfo.setOnClickListener(v -> showPersonalInfoDialog());

        btnAddress.setOnClickListener(v ->
                Toast.makeText(this, "Địa chỉ của tôi", Toast.LENGTH_SHORT).show());

        btnWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));

        // Click vào stat ví cũng mở WalletActivity
        statWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));

        btnOrderHistory.setOnClickListener(v ->
                startActivity(new Intent(this, OrderListActivity.class)));

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show());

        btnHelp.setOnClickListener(v ->
                Toast.makeText(this, "Trợ giúp", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showPersonalInfoDialog() {
        String name = sessionManager.getUserName();
        String phone = sessionManager.getUserPhone();
        String email = sessionManager.getUserEmail();
        String role = sessionManager.getUserRole();

        String roleName = "SHOPPER".equals(role) ? "Người đi chợ" : "Người mua";

        StringBuilder info = new StringBuilder();
        info.append("👤  Họ tên: ").append(name.isEmpty() ? "Chưa cập nhật" : name).append("\n\n");
        info.append("📱  Số điện thoại: ").append(phone.isEmpty() ? "Chưa cập nhật" : phone).append("\n\n");
        info.append("📧  Email: ").append(email == null || email.isEmpty() ? "Chưa cập nhật" : email).append("\n\n");
        info.append("🏷️  Vai trò: ").append(roleName);

        new AlertDialog.Builder(this)
                .setTitle("Thông tin cá nhân")
                .setMessage(info.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
