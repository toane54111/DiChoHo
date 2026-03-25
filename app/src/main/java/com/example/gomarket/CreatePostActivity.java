package com.example.gomarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etContent, etLocation;
    private RadioGroup rgCategory, rgRegion;
    private Spinner spinnerProvince;
    private ImageView ivImagePreview;
    private MaterialCardView cardImagePreview, btnPickImage;
    private ApiService apiService;
    private SessionManager session;
    private double latitude = 0, longitude = 0;

    private Uri selectedImageUri = null;
    private String uploadedImageUrl = null;

    private Map<String, List<String>> provincesMap = new LinkedHashMap<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        cardImagePreview.setVisibility(android.view.View.VISIBLE);
                        Glide.with(this).load(selectedImageUri).centerCrop().into(ivImagePreview);
                        uploadedImageUrl = null; // Will upload on submit
                    }
                }
            });

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
        ivImagePreview = findViewById(R.id.ivImagePreview);
        cardImagePreview = findViewById(R.id.cardImagePreview);
        btnPickImage = findViewById(R.id.btnPickImage);

        rgRegion.setOnCheckedChangeListener((group, checkedId) -> {
            String region = null;
            if (checkedId == R.id.rbMienBac) region = "MIEN_BAC";
            else if (checkedId == R.id.rbMienTrung) region = "MIEN_TRUNG";
            else if (checkedId == R.id.rbMienNam) region = "MIEN_NAM";
            updateProvinceSpinner(region);
        });

        updateProvinceSpinner(null);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPost).setOnClickListener(v -> submitPost());

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.btnRemoveImage).setOnClickListener(v -> {
            selectedImageUri = null;
            uploadedImageUrl = null;
            cardImagePreview.setVisibility(android.view.View.GONE);
        });

        // Handle prefill from AI Thổ Địa
        String prefillTitle = getIntent().getStringExtra("PREFILL_TITLE");
        if (prefillTitle != null) etTitle.setText(prefillTitle);
        String prefillCategory = getIntent().getStringExtra("PREFILL_CATEGORY");
        if ("gom_chung".equals(prefillCategory)) {
            rgCategory.check(R.id.rbGomChung);
        }

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

        findViewById(R.id.btnPost).setEnabled(false);

        // If image selected but not uploaded yet, upload first
        if (selectedImageUri != null && uploadedImageUrl == null) {
            uploadImageThenSubmit(title, content, locationName);
        } else {
            doSubmitPost(title, content, locationName);
        }
    }

    private void uploadImageThenSubmit(String title, String content, String locationName) {
        try {
            File tempFile = createTempFileFromUri(selectedImageUri);
            if (tempFile == null) {
                Toast.makeText(this, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnPost).setEnabled(true);
                return;
            }

            RequestBody reqBody = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", tempFile.getName(), reqBody);

            apiService.uploadImage(part).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedImageUrl = response.body().get("imageUrl");
                        doSubmitPost(title, content, locationName);
                    } else {
                        Toast.makeText(CreatePostActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show();
                        findViewById(R.id.btnPost).setEnabled(true);
                    }
                    tempFile.delete();
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(CreatePostActivity.this, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnPost).setEnabled(true);
                    tempFile.delete();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btnPost).setEnabled(true);
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void doSubmitPost(String title, String content, String locationName) {
        String category;
        int checkedId = rgCategory.getCheckedRadioButtonId();
        if (checkedId == R.id.rbDacSan) category = "dac_san";
        else if (checkedId == R.id.rbRaoVat) category = "rao_vat";
        else if (checkedId == R.id.rbGomChung) category = "gom_chung";
        else category = "nong_san";

        String region = null;
        int regionId = rgRegion.getCheckedRadioButtonId();
        if (regionId == R.id.rbMienBac) region = "MIEN_BAC";
        else if (regionId == R.id.rbMienTrung) region = "MIEN_TRUNG";
        else if (regionId == R.id.rbMienNam) region = "MIEN_NAM";

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
        if (uploadedImageUrl != null) {
            body.put("imageUrls", new ArrayList<>(Arrays.asList(uploadedImageUrl)));
        }

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
