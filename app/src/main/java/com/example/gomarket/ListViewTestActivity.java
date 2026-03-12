package com.example.gomarket;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ListViewTestActivity extends AppCompatActivity {

    private GridView gridView;
    private ArrayList<SanPham> danhSachSanPham;
    private SanPhamAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        // 1. Ánh xạ GridView
        gridView = findViewById(R.id.gridView);

// 2. Tạo dữ liệu (Tên, Giá gốc, Giá KM, Mô tả, Hình)
        danhSachSanPham = new ArrayList<>();
        danhSachSanPham.add(new SanPham(
                "Apple",
                "25.000đ",
                "20.000đ",
                "Táo Mỹ nhập khẩu, giòn ngọt, giàu vitamin C. Thích hợp ăn trực tiếp hoặc làm salad.",
                R.mipmap.ic_launcher
        ));
        danhSachSanPham.add(new SanPham(
                "Banana",
                "15.000đ",
                "12.000đ",
                "Chuối già Nam Mỹ, chín vàng đều, vị ngọt thanh. Giàu kali tốt cho tim mạch.",
                R.drawable.banana
        ));
        danhSachSanPham.add(new SanPham(
                "Cherry",
                "120.000đ",
                "99.000đ",
                "Cherry đỏ nhập khẩu Chile, quả to đều, vị ngọt chua. Giàu chất chống oxy hóa.",
                R.drawable.cherry
        ));
        danhSachSanPham.add(new SanPham(
                "Date",
                "80.000đ",
                "65.000đ",
                "Chà là khô Ả Rập, ngọt tự nhiên, giàu chất xơ. Tốt cho hệ tiêu hóa.",
                R.mipmap.ic_launcher
        ));
        danhSachSanPham.add(new SanPham(
                "Grapes",
                "45.000đ",
                "38.000đ",
                "Nho xanh không hạt Úc, giòn ngọt, vỏ mỏng. Ăn trực tiếp hoặc làm nước ép.",
                R.mipmap.ic_launcher
        ));
        // 3. Tạo Adapter
        adapter = new SanPhamAdapter(this, danhSachSanPham);

        // 4. Gắn Adapter vào GridView
        gridView.setAdapter(adapter);

// 5. Xử lý click - Chuyển sang màn hình chi tiết
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Lấy sản phẩm được click
                SanPham sp = danhSachSanPham.get(position);

                // Tạo Intent để chuyển sang ProductDetailActivity
                Intent intent = new Intent(ListViewTestActivity.this, ProductDetailActivity.class);

                // Gửi dữ liệu qua Intent
                intent.putExtra("tenSanPham", sp.getTenSanPham());
                intent.putExtra("giaGoc", sp.getGiaGoc());
                intent.putExtra("giaKhuyenMai", sp.getGiaKhuyenMai());
                intent.putExtra("moTa", sp.getMoTa());
                intent.putExtra("hinhAnh", sp.getHinhAnh());

                // Chuyển màn hình
                startActivity(intent);
            }
        });
    }
}