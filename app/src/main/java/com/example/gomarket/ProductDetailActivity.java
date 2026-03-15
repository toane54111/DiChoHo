package com.example.gomarket;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.gomarket.network.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private MaterialCardView btnBack, btnFavorite;
    private TextView tvProductName, tvGiaGoc, tvGiaKhuyenMai, tvMoTa, tvHeart;
    private TextView tvQuantity, btnMinus, btnPlus;
    private MaterialButton btnAddToCart;

    // Thông tin thêm - dynamic
    private TextView tvDiscount, tvRating, tvRatingCount;
    private TextView tvOrigin, tvWeight, tvExpiry;

    private int quantity = 1;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Ánh xạ view
        imgProduct = findViewById(R.id.imgProduct);
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        tvHeart = findViewById(R.id.tvHeart);
        tvProductName = findViewById(R.id.tvProductName);
        tvGiaGoc = findViewById(R.id.tvGiaGoc);
        tvGiaKhuyenMai = findViewById(R.id.tvGiaKhuyenMai);
        tvMoTa = findViewById(R.id.tvMoTa);
        tvQuantity = findViewById(R.id.tvQuantity);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        // Thông tin thêm
        tvDiscount = findViewById(R.id.tvDiscount);
        tvRating = findViewById(R.id.tvRating);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvWeight = findViewById(R.id.tvWeight);
        tvExpiry = findViewById(R.id.tvExpiry);

        // Nhận dữ liệu từ Intent
        String tenSP = getIntent().getStringExtra("tenSanPham");
        String giaGoc = getIntent().getStringExtra("giaGoc");
        String giaKM = getIntent().getStringExtra("giaKhuyenMai");
        String moTa = getIntent().getStringExtra("moTa");
        int hinhAnh = getIntent().getIntExtra("hinhAnh", R.mipmap.ic_launcher);

        // Dữ liệu bổ sung từ API
        double price = getIntent().getDoubleExtra("price", 0);
        double originalPrice = getIntent().getDoubleExtra("original_price", 0);
        String unit = getIntent().getStringExtra("unit");
        String category = getIntent().getStringExtra("category");
        int productId = getIntent().getIntExtra("product_id", -1);

        // Hiển thị dữ liệu cơ bản
        tvProductName.setText(tenSP);
        tvGiaGoc.setText(giaGoc);
        tvGiaKhuyenMai.setText(giaKM);
        tvMoTa.setText(moTa);

        // Ưu tiên ảnh URL từ API, fallback sang drawable local
        String apiImageUrl = ApiClient.getFullImageUrl(getIntent().getStringExtra("image_url"));
        if (apiImageUrl != null && !apiImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(apiImageUrl)
                    .apply(new RequestOptions()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .thumbnail(0.1f)
                    .placeholder(R.drawable.img)
                    .error(hinhAnh)
                    .into(imgProduct);
        } else {
            imgProduct.setImageResource(hinhAnh);
        }

        // Gạch ngang giá gốc
        tvGiaGoc.setPaintFlags(tvGiaGoc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // ═══════════════════════════════════════════
        // DYNAMIC: Thông tin thêm
        // ═══════════════════════════════════════════

        // 1. Badge giảm giá - tính từ originalPrice vs price
        setupDiscountBadge(price, originalPrice, giaGoc, giaKM);

        // 2. Rating - tạo pseudo-random ổn định theo productId
        setupRating(productId, tenSP);

        // 3. Khối lượng - dùng unit từ DB
        setupWeight(unit);

        // 4. Xuất xứ - suy luận từ category và tên SP
        setupOrigin(category, tenSP);

        // 5. Hạn sử dụng - suy luận từ category
        setupExpiry(category);

        // ═══════════════════════════════════════════
        // BUTTONS
        // ═══════════════════════════════════════════

        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            if (isFavorite) {
                tvHeart.setText("❤️");
                Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                tvHeart.setText("♡");
                Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        // Nút thêm vào giỏ hàng
        btnAddToCart.setOnClickListener(v -> {
            int pId = getIntent().getIntExtra("product_id", -1);
            double pPrice = getIntent().getDoubleExtra("price", 0);
            String imageUrl = getIntent().getStringExtra("image_url");

            // Fallback: parse price từ chuỗi giaKhuyenMai
            if (pPrice == 0 && giaKM != null) {
                try {
                    String cleaned = giaKM.replaceAll("[^\\d]", "");
                    if (!cleaned.isEmpty()) pPrice = Double.parseDouble(cleaned);
                } catch (NumberFormatException ignored) {}
            }

            // Fallback: nếu không có productId từ API
            if (pId == -1 && tenSP != null) {
                pId = Math.abs(tenSP.hashCode()) % 100000;
            }

            if (tenSP == null || tenSP.isEmpty()) {
                Toast.makeText(this, "Không thể thêm sản phẩm này vào giỏ!", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.gomarket.model.OrderItem item =
                    new com.example.gomarket.model.OrderItem(pId, tenSP, quantity, pPrice, imageUrl);
            CartActivity.addToCart(item);

            Toast.makeText(this,
                    "Đã thêm " + quantity + " " + tenSP + " vào giỏ hàng! 🛒",
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ═══════════════════════════════════════════
    // HELPER: Tính toán thông tin động
    // ═══════════════════════════════════════════

    /**
     * Tính % giảm giá từ originalPrice vs price.
     * Nếu không có giảm giá → ẩn badge + ẩn giá gốc gạch ngang.
     */
    private void setupDiscountBadge(double price, double originalPrice, String giaGoc, String giaKM) {
        if (originalPrice > 0 && price > 0 && originalPrice > price) {
            int discountPercent = (int) Math.round((1.0 - price / originalPrice) * 100);
            if (discountPercent > 0 && discountPercent < 100) {
                tvDiscount.setText("Giảm " + discountPercent + "%");
                tvDiscount.setVisibility(View.VISIBLE);
                tvGiaGoc.setVisibility(View.VISIBLE);
                return;
            }
        }

        // Fallback: thử parse từ chuỗi giá
        double parsedOriginal = parsePrice(giaGoc);
        double parsedPrice = parsePrice(giaKM);
        if (parsedOriginal > 0 && parsedPrice > 0 && parsedOriginal > parsedPrice) {
            int discountPercent = (int) Math.round((1.0 - parsedPrice / parsedOriginal) * 100);
            if (discountPercent > 0 && discountPercent < 100) {
                tvDiscount.setText("Giảm " + discountPercent + "%");
                tvDiscount.setVisibility(View.VISIBLE);
                tvGiaGoc.setVisibility(View.VISIBLE);
                return;
            }
        }

        // Không có giảm giá → ẩn badge và giá gốc
        tvDiscount.setVisibility(View.GONE);
        tvGiaGoc.setVisibility(View.GONE);
    }

    /**
     * Tạo rating pseudo-random ổn định theo productId.
     * Rating: 4.0 - 4.9, số đánh giá: 50 - 500.
     */
    private void setupRating(int productId, String tenSP) {
        int seed = productId > 0 ? productId : (tenSP != null ? Math.abs(tenSP.hashCode()) : 0);

        // Rating: 4.0 - 4.9 (BHX products thường được đánh giá cao)
        double rating = 4.0 + (seed % 10) / 10.0;
        // Số lượng đánh giá: 50 - 500
        int reviewCount = 50 + (seed % 451);

        tvRating.setText(String.format(Locale.US, "%.1f", rating));
        tvRatingCount.setText("(" + reviewCount + " đánh giá)");
    }

    /**
     * Hiển thị đơn vị/khối lượng từ DB.
     * Fallback: "Theo đơn vị bán"
     */
    private void setupWeight(String unit) {
        if (unit != null && !unit.trim().isEmpty()) {
            tvWeight.setText(unit);
        } else {
            tvWeight.setText("Theo đơn vị bán");
        }
    }

    /**
     * Suy luận xuất xứ từ category và tên sản phẩm.
     * Mặc định: "Việt Nam" (BHX chủ yếu hàng Việt)
     */
    private void setupOrigin(String category, String tenSP) {
        String origin = "Việt Nam";

        if (tenSP != null) {
            String lower = tenSP.toLowerCase();
            if (lower.contains("mỹ") || lower.contains("my")) {
                origin = "Mỹ";
            } else if (lower.contains("úc") || lower.contains("uc")) {
                origin = "Úc";
            } else if (lower.contains("nhật") || lower.contains("nhat")) {
                origin = "Nhật Bản";
            } else if (lower.contains("hàn") || lower.contains("han quoc")) {
                origin = "Hàn Quốc";
            } else if (lower.contains("chile")) {
                origin = "Chile";
            } else if (lower.contains("new zealand") || lower.contains("nz")) {
                origin = "New Zealand";
            } else if (lower.contains("nhập khẩu") || lower.contains("nhap khau")) {
                origin = "Nhập khẩu";
            }
        }

        tvOrigin.setText(origin);
    }

    /**
     * Suy luận hạn sử dụng từ category.
     * Thịt/cá tươi: ngắn, đồ khô: dài, rau: trung bình.
     */
    private void setupExpiry(String category) {
        String expiry = "Xem trên bao bì";

        if (category != null) {
            String lower = category.toLowerCase();
            if (lower.contains("thịt") || lower.contains("thit")) {
                expiry = "2-3 ngày (bảo quản lạnh)";
            } else if (lower.contains("cá") || lower.contains("hải sản") || lower.contains("hai san")) {
                expiry = "1-2 ngày (bảo quản lạnh)";
            } else if (lower.contains("rau") || lower.contains("củ") || lower.contains("cu qua")) {
                expiry = "3-5 ngày (bảo quản lạnh)";
            } else if (lower.contains("trái cây") || lower.contains("trai cay")) {
                expiry = "5-7 ngày";
            } else if (lower.contains("trứng") || lower.contains("trung")) {
                expiry = "14-21 ngày (bảo quản lạnh)";
            } else if (lower.contains("nấm") || lower.contains("nam")) {
                expiry = "3-5 ngày (bảo quản lạnh)";
            } else if (lower.contains("gia vị") || lower.contains("gia vi")) {
                expiry = "6-12 tháng";
            } else if (lower.contains("mì") || lower.contains("mi goi") || lower.contains("đồ khô")) {
                expiry = "6-12 tháng";
            }
        }

        tvExpiry.setText(expiry);
    }

    /**
     * Parse giá từ chuỗi định dạng "45.900đ" → 45900.0
     */
    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return 0;
        try {
            String cleaned = priceStr.replaceAll("[^\\d]", "");
            if (!cleaned.isEmpty()) return Double.parseDouble(cleaned);
        } catch (NumberFormatException ignored) {}
        return 0;
    }
}
