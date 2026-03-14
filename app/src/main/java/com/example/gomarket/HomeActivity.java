package com.example.gomarket;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.Product;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private GridView gridViewProducts;
    private ArrayList<SanPham> danhSachSanPham;
    private SanPhamAdapter adapter;

    // Bottom Navigation
    private LinearLayout navHome, navOrders, navCart, navProfile;

    // Header
    private MaterialCardView btnNotification, btnCart, searchBar, bannerSuggest;
    private TextView tvGreeting, tvAddress;

    // Categories
    private LinearLayout categoryVegetable, categoryMeat, categorySeafood, categoryFruit;
    private MaterialCardView btnDiChoGanNha, btnShopNoiBat;

    // API
    private ApiService apiService;
    private List<Product> apiProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        apiService = ApiClient.getApiService(this);

        initViews();
        loadUserGreeting();
        loadProducts();
        setupGridView();
        setupClickListeners();

        // Load sản phẩm từ API (sẽ cập nhật GridView khi có data)
        loadProductsFromApi();
    }

    private void initViews() {
        gridViewProducts = findViewById(R.id.gridViewProducts);
        btnNotification = findViewById(R.id.btnNotification);
        btnCart = findViewById(R.id.btnCart);
        searchBar = findViewById(R.id.searchBar);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvAddress = findViewById(R.id.tvAddress);

        // Categories
        categoryVegetable = findViewById(R.id.categoryVegetable);
        categoryMeat = findViewById(R.id.categoryMeat);
        categorySeafood = findViewById(R.id.categorySeafood);
        categoryFruit = findViewById(R.id.categoryFruit);

        // Bottom Navigation
        navHome = findViewById(R.id.navHome);
        navOrders = findViewById(R.id.navOrders);
        navCart = findViewById(R.id.navCart);
        navProfile = findViewById(R.id.navProfile);

        btnDiChoGanNha = findViewById(R.id.btnDiChoGanNha);
        btnShopNoiBat = findViewById(R.id.btnShopNoiBat);
        bannerSuggest = findViewById(R.id.bannerSuggest);
    }

    // ─── HIỂN THỊ TÊN USER TỪ SESSION ───
    private void loadUserGreeting() {
        SessionManager session = new SessionManager(this);
        String name = session.getUserName();
        if (name != null && !name.isEmpty() && !name.equals("Người dùng")) {
            tvGreeting.setText("Chào, " + name + "! 👋");
        } else {
            tvGreeting.setText("Chào bạn! 👋");
        }
    }

    // ─── LOAD SẢN PHẨM TỪ API ───
    private void loadProductsFromApi() {
        apiService.searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    apiProducts = response.body();
                    updateGridWithApiProducts();
                }
                // Nếu API thất bại, giữ data hardcoded
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Giữ data hardcoded khi không kết nối được server
            }
        });
    }

    private void updateGridWithApiProducts() {
        danhSachSanPham.clear();

        // Ảnh mặc định theo tên sản phẩm
        for (Product p : apiProducts) {
            int image = getProductImage(p.getName());
            String giaGoc = String.format("%,.0fđ", p.getOriginalPrice());
            String giaKM = String.format("%,.0fđ", p.getPrice());
            danhSachSanPham.add(new SanPham(
                    p.getName(), giaGoc, giaKM, p.getDescription(), image
            ));
        }

        adapter.notifyDataSetChanged();
    }

    // ─── LOAD SẢN PHẨM THEO DANH MỤC ───
    private void loadProductsByCategory(String category) {
        Toast.makeText(this, "Đang tải: " + category + "...", Toast.LENGTH_SHORT).show();

        apiService.getByCategory(category).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    apiProducts = response.body();
                    updateGridWithApiProducts();
                    Toast.makeText(HomeActivity.this,
                            "Đã tải " + apiProducts.size() + " sản phẩm " + category,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this,
                            "Không tìm thấy sản phẩm " + category,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(HomeActivity.this,
                        "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── MAP ẢNH LOCAL CHO SẢN PHẨM ───
    private int getProductImage(String productName) {
        String name = productName.toLowerCase();
        if (name.contains("táo")) return R.drawable.apple;
        if (name.contains("chuối")) return R.drawable.banana;
        if (name.contains("cherry")) return R.drawable.cherry;
        if (name.contains("nho")) return R.drawable.grapes;
        if (name.contains("xoài") || name.contains("chôm")) return R.drawable.date;
        // Default
        return R.drawable.img;
    }

    // ─── DATA HARDCODED (FALLBACK) ───
    private void loadProducts() {
        danhSachSanPham = new ArrayList<>();

        danhSachSanPham.add(new SanPham("Táo Mỹ", "25.000đ", "20.000đ",
                "Táo Mỹ nhập khẩu, giòn ngọt, giàu vitamin C.", R.drawable.apple));
        danhSachSanPham.add(new SanPham("Chuối già", "15.000đ", "12.000đ",
                "Chuối già Nam Mỹ, chín vàng đều, vị ngọt thanh.", R.drawable.banana));
        danhSachSanPham.add(new SanPham("Cherry Mỹ", "120.000đ", "99.000đ",
                "Cherry đỏ nhập khẩu Chile, quả to đều.", R.drawable.cherry));
        danhSachSanPham.add(new SanPham("Nho xanh Úc", "80.000đ", "65.000đ",
                "Nho xanh không hạt Úc, giòn ngọt.", R.drawable.grapes));
        danhSachSanPham.add(new SanPham("Xoài cát Hòa Lộc", "45.000đ", "38.000đ",
                "Xoài cát Hòa Lộc, thịt vàng ươm.", R.drawable.date));
        danhSachSanPham.add(new SanPham("Dưa hấu đỏ", "35.000đ", "28.000đ",
                "Dưa hấu ruột đỏ, ngọt mát.", R.drawable.date));
    }

    private void setupGridView() {
        adapter = new SanPhamAdapter(this, danhSachSanPham);
        gridViewProducts.setAdapter(adapter);

        gridViewProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SanPham sp = danhSachSanPham.get(position);

                Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
                intent.putExtra("tenSanPham", sp.getTenSanPham());
                intent.putExtra("giaGoc", sp.getGiaGoc());
                intent.putExtra("giaKhuyenMai", sp.getGiaKhuyenMai());
                intent.putExtra("moTa", sp.getMoTa());
                intent.putExtra("hinhAnh", sp.getHinhAnh());

                // Truyền thêm product ID từ API nếu có
                if (position < apiProducts.size()) {
                    Product apiProduct = apiProducts.get(position);
                    intent.putExtra("product_id", apiProduct.getId());
                    intent.putExtra("price", apiProduct.getPrice());
                }

                startActivity(intent);
            }
        });
    }

    private void setupClickListeners() {
        searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, IngredientSearchActivity.class);
            startActivity(intent);
        });

        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Mở thông báo", Toast.LENGTH_SHORT).show());

        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Categories → load sản phẩm từ API theo danh mục
        categoryVegetable.setOnClickListener(v -> loadProductsByCategory("Rau củ"));
        categoryMeat.setOnClickListener(v -> loadProductsByCategory("Thịt"));
        categorySeafood.setOnClickListener(v -> loadProductsByCategory("Hải sản"));
        categoryFruit.setOnClickListener(v -> loadProductsByCategory("Trái cây"));

        // Bottom Navigation
        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Đang ở Home", Toast.LENGTH_SHORT).show());

        navOrders.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OrderListActivity.class);
            startActivity(intent);
        });

        navCart.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnDiChoGanNha.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ShopperMapActivity.class);
            startActivity(intent);
        });

        btnShopNoiBat.setOnClickListener(v ->
                Toast.makeText(this, "Mở trang Shop nổi bật", Toast.LENGTH_SHORT).show());

        bannerSuggest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AIChefActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserGreeting(); // Cập nhật tên khi quay lại
    }
}
