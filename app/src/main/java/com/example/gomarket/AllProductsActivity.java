package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.Product;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllProductsActivity extends AppCompatActivity {

    private GridView gridView;
    private TextView tvBack, tvProductCount, tvEmptyState;
    private ArrayList<SanPham> danhSachSanPham = new ArrayList<>();
    private SanPhamAdapter adapter;
    private ApiService apiService;
    private List<Product> apiProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_products);

        apiService = ApiClient.getApiService(this);

        initViews();
        setupGrid();
        loadAllProducts();
    }

    private void initViews() {
        gridView = findViewById(R.id.gridViewAllProducts);
        tvBack = findViewById(R.id.tvBack);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tvBack.setOnClickListener(v -> finish());
    }

    private void setupGrid() {
        adapter = new SanPhamAdapter(this, danhSachSanPham);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            SanPham sp = danhSachSanPham.get(position);

            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("tenSanPham", sp.getTenSanPham());
            intent.putExtra("giaGoc", sp.getGiaGoc());
            intent.putExtra("giaKhuyenMai", sp.getGiaKhuyenMai());
            intent.putExtra("moTa", sp.getMoTa());
            intent.putExtra("hinhAnh", sp.getHinhAnh());

            if (position < apiProducts.size()) {
                Product apiProduct = apiProducts.get(position);
                intent.putExtra("product_id", apiProduct.getId());
                intent.putExtra("price", apiProduct.getPrice());
                intent.putExtra("original_price", apiProduct.getOriginalPrice());
                intent.putExtra("image_url", apiProduct.getImageUrl());
                intent.putExtra("unit", apiProduct.getUnit());
                intent.putExtra("category", apiProduct.getCategory());
            }

            startActivity(intent);
        });
    }

    private void loadAllProducts() {
        tvEmptyState.setText("Đang tải sản phẩm...");
        tvEmptyState.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);

        // Load tất cả sản phẩm (search với query rỗng)
        apiService.searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    apiProducts = response.body();
                    danhSachSanPham.clear();

                    for (Product p : apiProducts) {
                        String giaGoc = String.format("%,.0fđ", p.getOriginalPrice());
                        String giaKM = String.format("%,.0fđ", p.getPrice());
                        danhSachSanPham.add(new SanPham(
                                p.getName(), giaGoc, giaKM,
                                p.getDescription(), R.drawable.img,
                                p.getImageUrl()
                        ));
                    }

                    adapter.notifyDataSetChanged();
                    tvProductCount.setText(apiProducts.size() + " sản phẩm");
                    tvEmptyState.setVisibility(View.GONE);
                    gridView.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setText("Không tìm thấy sản phẩm nào");
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                tvEmptyState.setText("Lỗi kết nối server. Vui lòng thử lại!");
                Toast.makeText(AllProductsActivity.this,
                        "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
