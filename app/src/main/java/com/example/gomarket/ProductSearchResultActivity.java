package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
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

/**
 * Hiển thị kết quả Hybrid Search (Text + RAG/Vector)
 *
 * Flow: SearchActivity → gõ "đồ nấu canh chua" → ProductSearchResultActivity
 *   → Gọi /api/products/hybrid-search?q=đồ nấu canh chua
 *   → Text match (score 1.0) + Semantic match (score 0.6-0.9)
 *   → Hiển thị grid sản phẩm, click → ProductDetailActivity
 */
public class ProductSearchResultActivity extends AppCompatActivity {

    private GridView gridView;
    private TextView tvBack, tvSubtitle, tvEmptyState;
    private ProgressBar progressBar;
    private ArrayList<SanPham> danhSachSanPham = new ArrayList<>();
    private SanPhamAdapter adapter;
    private ApiService apiService;
    private List<Product> apiProducts = new ArrayList<>();

    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search_result);

        apiService = ApiClient.getApiService(this);

        // Lấy query từ SearchActivity
        searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        if (searchQuery == null) searchQuery = "";

        initViews();
        setupGrid();
        performHybridSearch(searchQuery);
    }

    private void initViews() {
        gridView = findViewById(R.id.gridViewProducts);
        tvBack = findViewById(R.id.tvBack);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);

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

    private void performHybridSearch(String query) {
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        gridView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvSubtitle.setText("Đang tìm \"" + query + "\"...");

        apiService.hybridSearch(query).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                progressBar.setVisibility(View.GONE);

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

                    // Count exact vs semantic
                    long exactCount = apiProducts.stream()
                            .filter(p -> "exact".equals(p.getMatchType()))
                            .count();
                    long semanticCount = apiProducts.size() - exactCount;

                    String subtitle = apiProducts.size() + " sản phẩm cho \"" + query + "\"";
                    if (exactCount > 0 && semanticCount > 0) {
                        subtitle += " (" + exactCount + " chính xác, " + semanticCount + " liên quan)";
                    }
                    tvSubtitle.setText(subtitle);

                    gridView.setVisibility(View.VISIBLE);
                } else {
                    tvSubtitle.setText("\"" + query + "\"");
                    tvEmptyState.setText("Không tìm thấy sản phẩm nào cho \"" + query + "\".\nThử từ khóa khác nhé!");
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvSubtitle.setText("\"" + query + "\"");
                tvEmptyState.setText("Lỗi kết nối server. Vui lòng thử lại!");
                tvEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(ProductSearchResultActivity.this,
                        "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
