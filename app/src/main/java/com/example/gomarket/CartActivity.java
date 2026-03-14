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

import com.example.gomarket.adapter.CartAdapter;
import com.example.gomarket.model.Order;
import com.example.gomarket.model.OrderItem;
import com.example.gomarket.model.OrderRequest;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemListener {

    private RecyclerView recyclerViewCart;
    private LinearLayout layoutEmpty, layoutBottom;
    private TextView tvCartCount, tvSubtotal, tvShippingFee, tvTotal;
    private MaterialButton btnOrder;
    private TextView btnBack;

    private List<OrderItem> cartItems;
    private CartAdapter adapter;
    private static final double SHIPPING_FEE = 15000;

    // Static cart để các Activity khác thêm vào
    private static List<OrderItem> globalCart = new ArrayList<>();

    public static void addToCart(OrderItem item) {
        // Kiểm tra đã có trong giỏ chưa
        for (OrderItem existing : globalCart) {
            if (existing.getProductId() == item.getProductId()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        globalCart.add(item);
    }

    public static List<OrderItem> getCart() {
        return globalCart;
    }

    public static void clearCart() {
        globalCart.clear();
    }

    public static int getCartSize() {
        return globalCart.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();
        cartItems = globalCart;

        setupRecyclerView();
        updateUI();
        setupClickListeners();
    }

    private void initViews() {
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutBottom = findViewById(R.id.layoutBottom);
        tvCartCount = findViewById(R.id.tvCartCount);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotal = findViewById(R.id.tvTotal);
        btnOrder = findViewById(R.id.btnOrder);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(cartItems, this);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(adapter);
    }

    private void updateUI() {
        if (cartItems.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewCart.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewCart.setVisibility(View.VISIBLE);
            layoutBottom.setVisibility(View.VISIBLE);

            double subtotal = 0;
            for (OrderItem item : cartItems) {
                subtotal += item.getSubtotal();
            }

            tvCartCount.setText(String.valueOf(cartItems.size()));
            tvSubtotal.setText(String.format("%,.0fđ", subtotal));
            tvShippingFee.setText(String.format("%,.0fđ", SHIPPING_FEE));
            tvTotal.setText(String.format("%,.0fđ", subtotal + SHIPPING_FEE));
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnOrder.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Chuyển sang CheckoutActivity thay vì đặt hàng trực tiếp
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }

    private void createOrder() {
        SessionManager session = new SessionManager(this);
        ApiService apiService = ApiClient.getApiService(this);

        List<OrderRequest.OrderItemRequest> items = new ArrayList<>();
        for (OrderItem item : cartItems) {
            items.add(new OrderRequest.OrderItemRequest(item.getProductId(), item.getQuantity()));
        }

        OrderRequest request = new OrderRequest(
                session.getUserId(),
                "Địa chỉ giao hàng", // TODO: lấy từ user
                10.7769, 106.7009, // TODO: lấy GPS thật
                items,
                "COD"
        );

        apiService.createOrder(request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order order = response.body();
                    Toast.makeText(CartActivity.this,
                            "Đặt hàng thành công! Đơn #" + order.getId(),
                            Toast.LENGTH_LONG).show();
                    clearCart();

                    // Chuyển sang OrderTrackingActivity
                    Intent intent = new Intent(CartActivity.this, OrderTrackingActivity.class);
                    intent.putExtra("order_id", order.getId());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CartActivity.this,
                            "Đặt hàng thất bại. Vui lòng thử lại!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                Toast.makeText(CartActivity.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onQuantityChanged(int position, int newQuantity) {
        updateUI();
    }

    @Override
    public void onItemRemoved(int position) {
        cartItems.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, cartItems.size());
        updateUI();
    }
}
