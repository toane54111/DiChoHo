package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.RegisterRequest;
import com.example.gomarket.model.User;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail, edtPassword, edtConfirmPassword;
    private RadioGroup radioGroupRole;
    private CheckBox checkBoxAgree;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.getApiService(this);

        // Ánh xạ view
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        checkBoxAgree = findViewById(R.id.checkBoxAgree);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Xử lý nút Đăng ký
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = edtFullName.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String confirmPassword = edtConfirmPassword.getText().toString().trim();

                // Kiểm tra validation
                if (fullName.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
                } else if (phone.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                } else if (!email.contains("@")) {
                    Toast.makeText(RegisterActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                } else if (!checkBoxAgree.isChecked()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng đồng ý điều khoản sử dụng", Toast.LENGTH_SHORT).show();
                } else {
                    // Lấy vai trò đã chọn
                    int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
                    String role = (selectedRoleId == R.id.radioBuyer) ? "BUYER" : "SHOPPER";

                    performRegister(fullName, phone, email, password, role);
                }
            }
        });

        // Xử lý chuyển sang Đăng nhập
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void performRegister(String fullName, String phone, String email,
                                  String password, String role) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        RegisterRequest request = new RegisterRequest(fullName, phone, email, password, role);

        apiService.register(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();

                    // Lưu session (bao gồm email) và chuyển thẳng vào Home
                    SessionManager session = new SessionManager(RegisterActivity.this);
                    session.saveLogin(
                            user.getToken(), user.getId(),
                            user.getFullName(), user.getPhone(),
                            user.getEmail(), user.getRole()
                    );

                    Toast.makeText(RegisterActivity.this,
                            "Đăng ký thành công! Chào mừng " + user.getFullName(),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Đăng ký thất bại! Số điện thoại có thể đã được sử dụng.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");
                Toast.makeText(RegisterActivity.this,
                        "Không thể kết nối server: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
