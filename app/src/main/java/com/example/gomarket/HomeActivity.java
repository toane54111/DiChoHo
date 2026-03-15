package com.example.gomarket;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

    // Categories - Row 1
    private LinearLayout categoryRauLa, categoryThitHeo, categoryCaHaiSan, categoryTraiCay;
    // Categories - Row 2
    private LinearLayout categoryRow2;
    private LinearLayout categoryCuQua, categoryThitBo, categoryThitGa, categoryXemThem;

    private MaterialCardView btnDiChoGanNha, btnShopNoiBat, btnSeeMoreProducts;
    private TextView tvSeeAll;

    // API
    private ApiService apiService;
    private List<Product> apiProducts = new ArrayList<>();

    // Badge giỏ hàng
    private TextView tvCartBadge;

    // Tất cả categories từ BHX data
    private static final String[][] ALL_CATEGORIES = {
            {"Rau lá", "🥬", "#E8F5E9"},
            {"Thịt heo", "🥩", "#FFEBEE"},
            {"Cá & Hải sản", "🐟", "#E3F2FD"},
            {"Trái cây", "🍎", "#FFF3E0"},
            {"Củ quả", "🥕", "#FFF8E1"},
            {"Thịt bò", "🥩", "#FCE4EC"},
            {"Thịt gà & vịt", "🍗", "#FBE9E7"},
            {"Nấm", "🍄", "#F3E5F5"},
            {"Trứng", "🥚", "#FFFDE7"},
    };

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

        // Load sản phẩm từ API
        loadProductsFromApi();
    }

    private void initViews() {
        gridViewProducts = findViewById(R.id.gridViewProducts);
        btnNotification = findViewById(R.id.btnNotification);
        btnCart = findViewById(R.id.btnCart);
        searchBar = findViewById(R.id.searchBar);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvAddress = findViewById(R.id.tvAddress);
        tvCartBadge = findViewById(R.id.tvCartBadge);

        // Categories - Row 1
        categoryRauLa = findViewById(R.id.categoryVegetable);
        categoryThitHeo = findViewById(R.id.categoryMeat);
        categoryCaHaiSan = findViewById(R.id.categorySeafood);
        categoryTraiCay = findViewById(R.id.categoryFruit);

        // Categories - Row 2
        categoryRow2 = findViewById(R.id.categoryRow2);
        categoryCuQua = findViewById(R.id.categoryCuQua);
        categoryThitBo = findViewById(R.id.categoryThitBo);
        categoryThitGa = findViewById(R.id.categoryThitGa);
        categoryXemThem = findViewById(R.id.categoryXemThem);

        // Bottom Navigation
        navHome = findViewById(R.id.navHome);
        navOrders = findViewById(R.id.navOrders);
        navCart = findViewById(R.id.navCart);
        navProfile = findViewById(R.id.navProfile);

        btnDiChoGanNha = findViewById(R.id.btnDiChoGanNha);
        btnShopNoiBat = findViewById(R.id.btnShopNoiBat);
        bannerSuggest = findViewById(R.id.bannerSuggest);

        // Sản phẩm nổi bật - xem tất cả
        tvSeeAll = findViewById(R.id.tvSeeAll);
        btnSeeMoreProducts = findViewById(R.id.btnSeeMoreProducts);
    }

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
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Giữ data hardcoded khi không kết nối được server
            }
        });
    }

    private static final int HOME_PRODUCT_LIMIT = 8; // Chỉ hiển thị 8 sản phẩm nổi bật trên Home

    private void updateGridWithApiProducts() {
        danhSachSanPham.clear();

        // Chỉ lấy tối đa HOME_PRODUCT_LIMIT sản phẩm cho trang chủ
        int limit = Math.min(apiProducts.size(), HOME_PRODUCT_LIMIT);
        for (int i = 0; i < limit; i++) {
            Product p = apiProducts.get(i);
            int image = R.drawable.img; // fallback
            String giaGoc = String.format("%,.0fđ", p.getOriginalPrice());
            String giaKM = String.format("%,.0fđ", p.getPrice());
            String imageUrl = p.getImageUrl();

            danhSachSanPham.add(new SanPham(
                    p.getName(), giaGoc, giaKM, p.getDescription(), image, imageUrl
            ));
        }

        adapter.notifyDataSetChanged();
        setGridViewHeightBasedOnChildren(gridViewProducts, 2);
    }

    /**
     * Fix GridView inside ScrollView: tính chiều cao dựa trên số item
     */
    private void setGridViewHeightBasedOnChildren(GridView gridView, int numColumns) {
        ListAdapter listAdapter = gridView.getAdapter();
        if (listAdapter == null) return;

        int totalHeight = 0;
        int items = listAdapter.getCount();
        int rows = (int) Math.ceil((double) items / numColumns);

        for (int i = 0; i < rows; i++) {
            View listItem = listAdapter.getView(i * numColumns, null, gridView);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(gridView.getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.UNSPECIFIED
            );
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight + (gridView.getVerticalSpacing() * (rows - 1)) + gridView.getPaddingTop() + gridView.getPaddingBottom();
        gridView.setLayoutParams(params);
        gridView.requestLayout();
    }

    // ─── MỞ TRANG DANH MỤC ───
    private void openCategory(String category) {
        Intent intent = new Intent(this, CategoryProductsActivity.class);
        intent.putExtra("category_name", category);
        startActivity(intent);
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

        // Fix GridView height inside ScrollView
        gridViewProducts.post(() -> setGridViewHeightBasedOnChildren(gridViewProducts, 2));

        gridViewProducts.setOnItemClickListener((parent, view, position, id) -> {
            SanPham sp = danhSachSanPham.get(position);

            Intent intent = new Intent(HomeActivity.this, ProductDetailActivity.class);
            intent.putExtra("tenSanPham", sp.getTenSanPham());
            intent.putExtra("giaGoc", sp.getGiaGoc());
            intent.putExtra("giaKhuyenMai", sp.getGiaKhuyenMai());
            intent.putExtra("moTa", sp.getMoTa());
            intent.putExtra("hinhAnh", sp.getHinhAnh());

            // Truyền thêm product ID + imageUrl + thông tin chi tiết từ API
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

    private void setupClickListeners() {
        searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Mở thông báo", Toast.LENGTH_SHORT).show());

        btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Categories Row 1 → mở trang danh mục riêng
        categoryRauLa.setOnClickListener(v -> openCategory("Rau lá"));
        categoryThitHeo.setOnClickListener(v -> openCategory("Thịt heo"));
        categoryCaHaiSan.setOnClickListener(v -> openCategory("Cá & Hải sản"));
        categoryTraiCay.setOnClickListener(v -> openCategory("Trái cây"));

        // Categories Row 2
        categoryCuQua.setOnClickListener(v -> openCategory("Củ quả"));
        categoryThitBo.setOnClickListener(v -> openCategory("Thịt bò"));
        categoryThitGa.setOnClickListener(v -> openCategory("Thịt gà & vịt"));

        // Xem thêm → hiện dialog chọn tất cả categories
        categoryXemThem.setOnClickListener(v -> showAllCategoriesDialog());

        // Bottom Navigation
        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Đang ở Trang chủ", Toast.LENGTH_SHORT).show());

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

        // "Xem tất cả" + "Xem thêm sản phẩm" → mở AllProductsActivity
        View.OnClickListener openAllProducts = v -> {
            Intent intent = new Intent(HomeActivity.this, AllProductsActivity.class);
            startActivity(intent);
        };
        tvSeeAll.setOnClickListener(openAllProducts);
        btnSeeMoreProducts.setOnClickListener(openAllProducts);
    }

    /**
     * Hiện dialog chọn tất cả categories
     */
    private void showAllCategoriesDialog() {
        String[] categoryNames = new String[ALL_CATEGORIES.length];
        String[] categoryEmojis = new String[ALL_CATEGORIES.length];
        for (int i = 0; i < ALL_CATEGORIES.length; i++) {
            categoryEmojis[i] = ALL_CATEGORIES[i][1];
            categoryNames[i] = ALL_CATEGORIES[i][1] + "  " + ALL_CATEGORIES[i][0];
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn danh mục")
                .setItems(categoryNames, (dialog, which) -> {
                    openCategory(ALL_CATEGORIES[which][0]);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserGreeting();
        updateCartBadge();
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        int size = CartActivity.getCartSize();
        if (size > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(size > 99 ? "99+" : String.valueOf(size));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }
}
