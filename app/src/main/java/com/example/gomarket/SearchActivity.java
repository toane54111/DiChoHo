package com.example.gomarket;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearch);

        // Setup tag click listeners
        setupTagClick(R.id.tag_thit_kho, "Thịt kho");
        setupTagClick(R.id.tag_ca_kho, "Cá kho");
        setupTagClick(R.id.tag_bun_rieu, "Bún riêu");
        setupTagClick(R.id.tag_com_tam, "Cơm tấm");
        setupTagClick(R.id.tag_pho_bo, "Phở bò");
        setupTagClick(R.id.tag_banh_mi, "Bánh mì");
    }

    private void setupTagClick(int tagId, String searchText) {
        View tagView = findViewById(tagId);
        if (tagView != null) {
            tagView.setOnClickListener(v -> {
                etSearch.setText(searchText);
                etSearch.setSelection(searchText.length());
                performSearch(searchText);
            });
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Đang tìm kiếm: " + query, Toast.LENGTH_SHORT).show();
    }
}
