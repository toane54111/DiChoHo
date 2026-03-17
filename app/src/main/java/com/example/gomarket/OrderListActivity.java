package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.ShoppingRequestAdapter;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListActivity extends AppCompatActivity
        implements ShoppingRequestAdapter.OnRequestClickListener {

    private RecyclerView recyclerViewOrders;
    private LinearLayout layoutEmpty;
    private MaterialButton btnFilterAll, btnFilterPending, btnFilterCompleted;

    private List<ShoppingRequest> allRequests = new ArrayList<>();
    private ShoppingRequestAdapter adapter;
    private ApiService apiService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        session = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void initViews() {
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterPending = findViewById(R.id.btnFilterPending);
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted);
    }

    private void setupRecyclerView() {
        adapter = new ShoppingRequestAdapter(allRequests, this);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(adapter);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnFilterAll.setOnClickListener(v -> {
            setActiveFilter(btnFilterAll);
            adapter.updateRequests(allRequests);
            updateEmptyState(allRequests);
        });

        btnFilterPending.setOnClickListener(v -> {
            setActiveFilter(btnFilterPending);
            List<ShoppingRequest> filtered = new ArrayList<>();
            for (ShoppingRequest req : allRequests) {
                String s = req.getStatus();
                if (!"COMPLETED".equals(s) && !"CANCELLED".equals(s)) {
                    filtered.add(req);
                }
            }
            adapter.updateRequests(filtered);
            updateEmptyState(filtered);
        });

        btnFilterCompleted.setOnClickListener(v -> {
            setActiveFilter(btnFilterCompleted);
            List<ShoppingRequest> filtered = new ArrayList<>();
            for (ShoppingRequest req : allRequests) {
                if ("COMPLETED".equals(req.getStatus())) {
                    filtered.add(req);
                }
            }
            adapter.updateRequests(filtered);
            updateEmptyState(filtered);
        });
    }

    private void setActiveFilter(MaterialButton active) {
        btnFilterAll.setBackgroundTintList(getColorStateList(
                active == btnFilterAll ? R.color.primary : R.color.white));
        btnFilterAll.setTextColor(getColor(
                active == btnFilterAll ? R.color.white : R.color.text_primary));

        btnFilterPending.setBackgroundTintList(getColorStateList(
                active == btnFilterPending ? R.color.primary : R.color.white));
        btnFilterPending.setTextColor(getColor(
                active == btnFilterPending ? R.color.white : R.color.text_primary));

        btnFilterCompleted.setBackgroundTintList(getColorStateList(
                active == btnFilterCompleted ? R.color.primary : R.color.white));
        btnFilterCompleted.setTextColor(getColor(
                active == btnFilterCompleted ? R.color.white : R.color.text_primary));
    }

    private void loadRequests() {
        long userId = session.getUserId();

        apiService.getUserShoppingRequests(userId).enqueue(new Callback<List<ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<ShoppingRequest>> call,
                                   Response<List<ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRequests.clear();
                    allRequests.addAll(response.body());
                    adapter.updateRequests(allRequests);
                }
                updateEmptyState(allRequests);
            }

            @Override
            public void onFailure(Call<List<ShoppingRequest>> call, Throwable t) {
                Toast.makeText(OrderListActivity.this,
                        "Không thể tải danh sách đơn", Toast.LENGTH_SHORT).show();
                updateEmptyState(allRequests);
            }
        });
    }

    private void updateEmptyState(List<ShoppingRequest> list) {
        if (list.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewOrders.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestClick(ShoppingRequest request) {
        Intent intent = new Intent(this, OrderWaitingActivity.class);
        intent.putExtra("REQUEST_ID", request.getId());
        startActivity(intent);
    }
}
