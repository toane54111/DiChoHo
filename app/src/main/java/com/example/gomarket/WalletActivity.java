package com.example.gomarket;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.TransactionAdapter;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.model.WalletTransaction;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBack, tvBalance, tvBalanceLabel;
    private MaterialButton btnTopUp50, btnTopUp100, btnTopUp200, btnTopUp500;
    private RecyclerView rvTransactions;
    private TextView tvEmptyTransactions;

    private ApiService apiService;
    private SessionManager session;
    private long userId;

    private List<WalletTransaction> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);
        userId = session.getUserId();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBalance();
        loadTransactions();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tvBack);
        tvBalance = findViewById(R.id.tvBalance);
        tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
        btnTopUp50 = findViewById(R.id.btnTopUp50);
        btnTopUp100 = findViewById(R.id.btnTopUp100);
        btnTopUp200 = findViewById(R.id.btnTopUp200);
        btnTopUp500 = findViewById(R.id.btnTopUp500);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmptyTransactions = findViewById(R.id.tvEmptyTransactions);

        // Setup RecyclerView
        transactionAdapter = new TransactionAdapter(transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);

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
                    tvBalance.setText(String.format("%,d", balance) + "d");
                } else {
                    tvBalance.setText("0d");
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvBalance.setText("Loi ket noi");
                Toast.makeText(WalletActivity.this,
                        "Khong the tai vi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactions() {
        apiService.getWalletTransactions(userId).enqueue(new Callback<List<WalletTransaction>>() {
            @Override
            public void onResponse(Call<List<WalletTransaction>> call,
                                   Response<List<WalletTransaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    transactionList.clear();
                    transactionList.addAll(response.body());
                    transactionAdapter.notifyDataSetChanged();

                    if (transactionList.isEmpty()) {
                        tvEmptyTransactions.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        tvEmptyTransactions.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<WalletTransaction>> call, Throwable t) {
                tvEmptyTransactions.setVisibility(View.VISIBLE);
                tvEmptyTransactions.setText("Khong the tai lich su");
                rvTransactions.setVisibility(View.GONE);
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
                    tvBalance.setText(String.format("%,d", balance) + "d");
                    Toast.makeText(WalletActivity.this,
                            "Nap " + String.format("%,d", amount) + "d thanh cong!",
                            Toast.LENGTH_SHORT).show();
                    loadTransactions(); // Refresh lịch sử
                } else {
                    String errorMsg = "Nap tien that bai!";
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
                        "Loi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
