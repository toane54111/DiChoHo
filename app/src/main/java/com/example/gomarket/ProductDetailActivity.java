package com.example.gomarket;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private MaterialCardView btnBack, btnFavorite;
    private TextView tvProductName, tvGiaGoc, tvGiaKhuyenMai, tvMoTa, tvHeart;
    private TextView tvQuantity, btnMinus, btnPlus;
    private MaterialButton btnAddToCart;

    private int quantity = 1;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Ánh xạ view
        imgProduct = findViewById(R.id.id_anh_san_pham); // Trỏ đúng vào ImageView
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

        // Nhận dữ liệu từ Intent
        String tenSP = getIntent().getStringExtra("tenSanPham");
        String giaGoc = getIntent().getStringExtra("giaGoc");
        String giaKM = getIntent().getStringExtra("giaKhuyenMai");
        String moTa = getIntent().getStringExtra("moTa");
        int hinhAnh = getIntent().getIntExtra("hinhAnh", R.mipmap.ic_launcher);

        // Hiển thị dữ liệu
        tvProductName.setText(tenSP);
        tvGiaGoc.setText(giaGoc);
        tvGiaKhuyenMai.setText(giaKM);
        tvMoTa.setText(moTa);
        imgProduct.setImageResource(hinhAnh);

        // Gạch ngang giá gốc
        tvGiaGoc.setPaintFlags(tvGiaGoc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // Nút quay lại
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Nút yêu thích
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFavorite = !isFavorite;
                if (isFavorite) {
                    tvHeart.setText("❤️");
                    Toast.makeText(ProductDetailActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                } else {
                    tvHeart.setText("♡");
                    Toast.makeText(ProductDetailActivity.this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Nút giảm số lượng
        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 1) {
                    quantity--;
                    tvQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        // Nút tăng số lượng
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        // Nút thêm vào giỏ hàng
        btnAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProductDetailActivity.this,
                        "Đã thêm " + quantity + " " + tenSP + " vào giỏ hàng!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}