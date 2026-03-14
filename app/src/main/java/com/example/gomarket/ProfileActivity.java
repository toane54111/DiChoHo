package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout btnPersonalInfo, btnAddress, btnWallet, btnOrderHistory, btnSettings, btnHelp;
    private MaterialButton btnLogout;
    private TextView tvName;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        // Ánh xạ view
        tvName = findViewById(R.id.tvName);
        btnPersonalInfo = findViewById(R.id.btnPersonalInfo);
        btnAddress = findViewById(R.id.btnAddress);
        btnWallet = findViewById(R.id.btnWallet);
        btnOrderHistory = findViewById(R.id.btnOrderHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHelp = findViewById(R.id.btnHelp);
        btnLogout = findViewById(R.id.btnLogout);

        // Hiển thị tên user từ session
        String userName = sessionManager.getUserName();
        if (!userName.isEmpty()) {
            tvName.setText(userName);
        }

        // Xử lý click
        btnPersonalInfo.setOnClickListener(v ->
                Toast.makeText(this, "Thông tin cá nhân", Toast.LENGTH_SHORT).show());

        btnAddress.setOnClickListener(v ->
                Toast.makeText(this, "Địa chỉ của tôi", Toast.LENGTH_SHORT).show());

        btnWallet.setOnClickListener(v -> {
            Intent intent = new Intent(this, WalletActivity.class);
            startActivity(intent);
        });

        btnOrderHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderListActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show());

        btnHelp.setOnClickListener(v ->
                Toast.makeText(this, "Trợ giúp", Toast.LENGTH_SHORT).show());

        // Đăng xuất - xóa session
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}