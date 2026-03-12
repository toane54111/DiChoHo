package com.example.gomarket;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // UI hiện tại là static layout với gợi ý tìm kiếm và danh mục
        // Có thể mở rộng thêm logic tìm kiếm sau
        Toast.makeText(this, "Tìm kiếm sản phẩm", Toast.LENGTH_SHORT).show();
    }
}
