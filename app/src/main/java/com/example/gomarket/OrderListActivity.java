package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.OrderAdapter;
import com.example.gomarket.model.Order;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {

    private RecyclerView recyclerViewOrders;
    private LinearLayout layoutEmpty;
    private MaterialButton btnFilterAll, btnFilterPending, btnFilterCompleted;

    private List<Order> allOrders = new ArrayList<>();
    private OrderAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadOrders();
    }

    private void initViews() {
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterPending = findViewById(R.id.btnFilterPending);
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted);
        apiService = ApiClient.getApiService(this);
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(allOrders, this);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(adapter);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnFilterAll.setOnClickListener(v -> filterOrders("ALL"));
        btnFilterPending.setOnClickListener(v -> filterOrders("PENDING"));
        btnFilterCompleted.setOnClickListener(v -> filterOrders("COMPLETED"));
    }

    private void loadOrders() {
        SessionManager session = new SessionManager(this);
        int userId = session.getUserId();

        apiService.getUserOrders(userId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allOrders.clear();
                    allOrders.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(OrderListActivity.this,
                        "Không thể tải đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterOrders(String filter) {
        if ("ALL".equals(filter)) {
            adapter.updateOrders(allOrders);
        } else {
            List<Order> filtered = new ArrayList<>();
            for (Order order : allOrders) {
                if ("PENDING".equals(filter)) {
                    if (!"COMPLETED".equals(order.getStatus())) {
                        filtered.add(order);
                    }
                } else if ("COMPLETED".equals(filter)) {
                    if ("COMPLETED".equals(order.getStatus())) {
                        filtered.add(order);
                    }
                }
            }
            adapter.updateOrders(filtered);
        }
    }

    private void updateEmptyState() {
        if (allOrders.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewOrders.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("order_id", order.getId());
        startActivity(intent);
    }
}
