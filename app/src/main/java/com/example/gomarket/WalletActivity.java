package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.model.Wallet;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBack, tvBalance, tvBalanceLabel;
    private MaterialButton btnTopUp50, btnTopUp100, btnTopUp200, btnTopUp500;
    private LinearLayout layoutTopUp;
    private RecyclerView rvTransactions;

    private ApiService apiService;
    private SessionManager session;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);
        userId = session.getUserId();

        initViews();
        loadBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance(); // Reload khi quay lại sau QR
    }

    private void initViews() {
        tvBack = findViewById(R.id.tvBack);
        tvBalance = findViewById(R.id.tvBalance);
        tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
        btnTopUp50 = findViewById(R.id.btnTopUp50);
        btnTopUp100 = findViewById(R.id.btnTopUp100);
        btnTopUp200 = findViewById(R.id.btnTopUp200);
        btnTopUp500 = findViewById(R.id.btnTopUp500);

        tvBack.setOnClickListener(v -> finish());

        btnTopUp50.setOnClickListener(v -> topUp(50_000L));
        btnTopUp100.setOnClickListener(v -> topUp(100_000L));
        btnTopUp200.setOnClickListener(v -> topUp(200_000L));
        btnTopUp500.setOnClickListener(v -> topUp(500_000L));
    }

    private void loadBalance() {
        apiService.getWalletBalance(userId).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    tvBalance.setText(String.format("%,dđ", balance));
                } else {
                    tvBalance.setText("0đ");
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvBalance.setText("Lỗi kết nối");
                Toast.makeText(WalletActivity.this,
                        "Không thể tải ví: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void topUp(long amount) {
        Map<String, Long> body = Map.of("amount", amount);
        apiService.topUpWallet(userId, body).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    tvBalance.setText(String.format("%,dđ", balance));
                    Toast.makeText(WalletActivity.this,
                            "Nạp " + String.format("%,d", amount) + "đ thành công! 💰",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = "Nạp tiền thất bại!";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(WalletActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                Toast.makeText(WalletActivity.this,
                        "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
