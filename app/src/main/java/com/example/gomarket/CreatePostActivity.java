package com.example.gomarket;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etContent, etLocation;
    private RadioGroup rgCategory, rgRegion;
    private Spinner spinnerProvince;
    private ApiService apiService;
    private SessionManager session;
    private double latitude = 0, longitude = 0;

    private Map<String, List<String>> provincesMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        initProvincesData();

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        etLocation = findViewById(R.id.etLocation);
        rgCategory = findViewById(R.id.rgCategory);
        rgRegion = findViewById(R.id.rgRegion);
        spinnerProvince = findViewById(R.id.spinnerProvince);

        // Update province spinner when region changes
        rgRegion.setOnCheckedChangeListener((group, checkedId) -> {
            String region = null;
            if (checkedId == R.id.rbMienBac) region = "MIEN_BAC";
            else if (checkedId == R.id.rbMienTrung) region = "MIEN_TRUNG";
            else if (checkedId == R.id.rbMienNam) region = "MIEN_NAM";
            updateProvinceSpinner(region);
        });

        // Initialize with empty spinner
        updateProvinceSpinner(null);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPost).setOnClickListener(v -> submitPost());

        getCurrentLocation();
    }

    private void initProvincesData() {
        provincesMap.put("MIEN_BAC", Arrays.asList(
            "-- Chọn tỉnh --", "Hà Nội", "Hải Phòng", "Quảng Ninh", "Bắc Giang", "Bắc Kạn", "Bắc Ninh",
            "Cao Bằng", "Điện Biên", "Hà Giang", "Hà Nam", "Hải Dương", "Hòa Bình",
            "Hưng Yên", "Lai Châu", "Lạng Sơn", "Lào Cai", "Nam Định", "Ninh Bình",
            "Phú Thọ", "Sơn La", "Thái Bình", "Thái Nguyên", "Tuyên Quang", "Vĩnh Phúc", "Yên Bái"
        ));
        provincesMap.put("MIEN_TRUNG", Arrays.asList(
            "-- Chọn tỉnh --", "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Quảng Bình", "Quảng Trị",
            "Thừa Thiên Huế", "Đà Nẵng", "Quảng Nam", "Quảng Ngãi", "Bình Định",
            "Phú Yên", "Khánh Hòa", "Ninh Thuận", "Bình Thuận",
            "Kon Tum", "Gia Lai", "Đắk Lắk", "Đắk Nông", "Lâm Đồng"
        ));
        provincesMap.put("MIEN_NAM", Arrays.asList(
            "-- Chọn tỉnh --", "TP. Hồ Chí Minh", "Bà Rịa - Vũng Tàu", "Bình Dương", "Bình Phước",
            "Đồng Nai", "Tây Ninh", "Long An", "Tiền Giang", "Bến Tre", "Trà Vinh",
            "Vĩnh Long", "Đồng Tháp", "An Giang", "Kiên Giang", "Cần Thơ",
            "Hậu Giang", "Sóc Trăng", "Bạc Liêu", "Cà Mau"
        ));
    }

    private void updateProvinceSpinner(String region) {
        List<String> provinces;
        if (region != null && provincesMap.containsKey(region)) {
            provinces = provincesMap.get(region);
        } else {
            provinces = Arrays.asList("-- Chọn vùng miền trước --");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, provinces);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(adapter);
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

        // Determine region
        String region = null;
        int regionId = rgRegion.getCheckedRadioButtonId();
        if (regionId == R.id.rbMienBac) region = "MIEN_BAC";
        else if (regionId == R.id.rbMienTrung) region = "MIEN_TRUNG";
        else if (regionId == R.id.rbMienNam) region = "MIEN_NAM";

        // Determine province
        String province = null;
        if (spinnerProvince.getSelectedItemPosition() > 0) {
            province = (String) spinnerProvince.getSelectedItem();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", (long) session.getUserId());
        body.put("title", title);
        body.put("content", content);
        body.put("category", category);
        if (region != null) body.put("region", region);
        if (province != null) body.put("province", province);
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
