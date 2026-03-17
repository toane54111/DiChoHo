package com.example.gomarket;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etContent, etLocation;
    private RadioGroup rgCategory;
    private ApiService apiService;
    private SessionManager session;
    private double latitude = 0, longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        etLocation = findViewById(R.id.etLocation);
        rgCategory = findViewById(R.id.rgCategory);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPost).setOnClickListener(v -> submitPost());

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        });
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String locationName = etLocation.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }
        if (content.isEmpty()) {
            etContent.setError("Vui lòng nhập nội dung");
            return;
        }

        String category;
        int checkedId = rgCategory.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDacSan) category = "dac_san";
        else if (checkedId == R.id.rbRaoVat) category = "rao_vat";
        else if (checkedId == R.id.rbGomChung) category = "gom_chung";
        else category = "nong_san";

        Map<String, Object> body = new HashMap<>();
        body.put("userId", (long) session.getUserId());
        body.put("title", title);
        body.put("content", content);
        body.put("category", category);
        if (!locationName.isEmpty()) body.put("locationName", locationName);
        if (latitude != 0) {
            body.put("latitude", latitude);
            body.put("longitude", longitude);
        }

        findViewById(R.id.btnPost).setEnabled(false);

        apiService.createPost(body).enqueue(new Callback<CommunityPost>() {
            @Override
            public void onResponse(Call<CommunityPost> call, Response<CommunityPost> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreatePostActivity.this,
                            "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreatePostActivity.this,
                            "Lỗi đăng bài", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnPost).setEnabled(true);
                }
            }
            @Override
            public void onFailure(Call<CommunityPost> call, Throwable t) {
                Toast.makeText(CreatePostActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnPost).setEnabled(true);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}
