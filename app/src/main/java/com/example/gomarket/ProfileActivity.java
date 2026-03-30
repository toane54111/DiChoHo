package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.Order;
import com.example.gomarket.model.User;
import com.example.gomarket.model.Wallet;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout btnPersonalInfo, btnAddress, btnWallet, btnOrderHistory, btnSettings, btnHelp;
    private MaterialButton btnLogout;
    private TextView tvName, tvPhone, tvEmail, tvRole, tvRoleIcon;
    private TextView tvStatOrders, tvStatBalance, tvStatCompleted;
    private LinearLayout statWallet;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupUserInfo();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileFromServer();
        loadStats();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvRole = findViewById(R.id.tvRole);
        tvRoleIcon = findViewById(R.id.tvRoleIcon);
        tvStatOrders = findViewById(R.id.tvStatOrders);
        tvStatBalance = findViewById(R.id.tvStatBalance);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);
        statWallet = findViewById(R.id.statWallet);
        btnPersonalInfo = findViewById(R.id.btnPersonalInfo);
        btnAddress = findViewById(R.id.btnAddress);
        btnWallet = findViewById(R.id.btnWallet);
        btnOrderHistory = findViewById(R.id.btnOrderHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnHelp = findViewById(R.id.btnHelp);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupUserInfo() {
        String userName = sessionManager.getUserName();
        if (!userName.isEmpty()) {
            tvName.setText(userName);
        }

        String phone = sessionManager.getUserPhone();
        if (!phone.isEmpty()) {
            tvPhone.setText(phone);
        }

        // Hiển thị email
        String email = sessionManager.getUserEmail();
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }

        // Hiển thị badge vai trò
        String role = sessionManager.getUserRole();
        if ("SHOPPER".equals(role)) {
            tvRoleIcon.setText("🛍️");
            tvRole.setText("Người đi chợ");
        } else {
            tvRoleIcon.setText("🛒");
            tvRole.setText("Người mua");
        }
    }

    private void loadProfileFromServer() {
        long userId = sessionManager.getUserId();
        if (userId <= 0) return;

        apiService.getProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    // Cập nhật session với dữ liệu mới nhất từ server
                    sessionManager.saveLogin(
                            sessionManager.getToken(),
                            user.getId(),
                            user.getFullName(),
                            user.getPhone(),
                            user.getEmail(),
                            user.getRole()
                    );
                    // Cập nhật UI
                    setupUserInfo();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Giữ dữ liệu local nếu không kết nối được server
            }
        });
    }

    private void loadStats() {
        long userId = sessionManager.getUserId();

        // Load số đơn đi chợ hộ
        apiService.getUserShoppingRequests(userId).enqueue(new Callback<List<com.example.gomarket.model.ShoppingRequest>>() {
            @Override
            public void onResponse(Call<List<com.example.gomarket.model.ShoppingRequest>> call,
                                   Response<List<com.example.gomarket.model.ShoppingRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    var requests = response.body();
                    tvStatOrders.setText(String.valueOf(requests.size()));

                    int completed = 0;
                    for (var req : requests) {
                        if ("COMPLETED".equals(req.getStatus())) completed++;
                    }
                    tvStatCompleted.setText(String.valueOf(completed));
                } else {
                    tvStatOrders.setText("0");
                    tvStatCompleted.setText("0");
                }
            }

            @Override
            public void onFailure(Call<List<com.example.gomarket.model.ShoppingRequest>> call, Throwable t) {
                tvStatOrders.setText("0");
                tvStatCompleted.setText("0");
            }
        });

        // Load số dư ví
        apiService.getWalletBalance(userId).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long balance = response.body().getBalance();
                    if (balance >= 1_000_000) {
                        tvStatBalance.setText(String.format("%.1fM", balance / 1_000_000.0));
                    } else if (balance >= 1_000) {
                        tvStatBalance.setText(String.format("%dK", balance / 1_000));
                    } else {
                        tvStatBalance.setText(String.format("%,d", balance));
                    }
                } else {
                    tvStatBalance.setText("0");
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                tvStatBalance.setText("0");
            }
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPersonalInfo.setOnClickListener(v -> showPersonalInfoDialog());

        btnAddress.setOnClickListener(v -> showAddressDialog());

        btnWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));

        // Click vào stat ví cũng mở WalletActivity
        statWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));

        btnOrderHistory.setOnClickListener(v ->
                startActivity(new Intent(this, OrderListActivity.class)));

        btnSettings.setOnClickListener(v -> showSettingsDialog());

        btnHelp.setOnClickListener(v -> showHelpDialog());

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showAddressDialog() {
        String name = sessionManager.getUserName();

        StringBuilder info = new StringBuilder();
        info.append("📍  Địa chỉ mặc định:\n");
        info.append("     Chưa thiết lập\n\n");
        info.append("🏠  Địa chỉ nhà:\n");
        info.append("     Chưa thiết lập\n\n");
        info.append("🏢  Địa chỉ công ty:\n");
        info.append("     Chưa thiết lập\n\n");
        info.append("💡  Mẹo: Khi tạo đơn đi chợ hộ, bạn có thể chọn vị trí giao trên bản đồ.");

        new AlertDialog.Builder(this)
                .setTitle("Địa chỉ của tôi")
                .setMessage(info.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showSettingsDialog() {
        String[] items = {
                "🔔  Thông báo đẩy                    Bật",
                "🌙  Chế độ tối                              Tắt",
                "🌐  Ngôn ngữ                             Tiếng Việt",
                "📍  Chia sẻ vị trí                          Bật",
                "💾  Xóa bộ nhớ đệm",
                "📋  Điều khoản sử dụng",
                "🔒  Chính sách bảo mật"
        };

        new AlertDialog.Builder(this)
                .setTitle("Cài đặt")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Thông báo đẩy: đang bật", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(this, "Chế độ tối chưa hỗ trợ", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, "Hiện chỉ hỗ trợ Tiếng Việt", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(this, "Chia sẻ vị trí: đang bật", Toast.LENGTH_SHORT).show();
                            break;
                        case 4:
                            Toast.makeText(this, "Đã xóa bộ nhớ đệm", Toast.LENGTH_SHORT).show();
                            break;
                        case 5:
                        case 6:
                            showTermsDialog(which == 6);
                            break;
                    }
                })
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showTermsDialog(boolean isPrivacy) {
        String title = isPrivacy ? "Chính sách bảo mật" : "Điều khoản sử dụng";
        String content;
        if (isPrivacy) {
            content = "🔒 CHÍNH SÁCH BẢO MẬT - GoMarket\n\n" +
                    "1. Thu thập dữ liệu\n" +
                    "Chúng tôi thu thập thông tin cá nhân (tên, SĐT, email) và vị trí để cung cấp dịch vụ đi chợ hộ.\n\n" +
                    "2. Sử dụng dữ liệu\n" +
                    "Dữ liệu được sử dụng để: xử lý đơn hàng, kết nối người mua - shopper, gợi ý sản phẩm phù hợp.\n\n" +
                    "3. Bảo mật\n" +
                    "Thông tin của bạn được mã hóa và bảo vệ. Chúng tôi không chia sẻ dữ liệu với bên thứ ba.\n\n" +
                    "4. Quyền của bạn\n" +
                    "Bạn có quyền xem, chỉnh sửa hoặc xóa dữ liệu cá nhân bất cứ lúc nào.";
        } else {
            content = "📋 ĐIỀU KHOẢN SỬ DỤNG - GoMarket\n\n" +
                    "1. Dịch vụ\n" +
                    "GoMarket cung cấp nền tảng kết nối người mua hàng với người đi chợ hộ (shopper).\n\n" +
                    "2. Tài khoản\n" +
                    "Người dùng chịu trách nhiệm bảo mật tài khoản và mọi hoạt động trên tài khoản.\n\n" +
                    "3. Thanh toán\n" +
                    "Hỗ trợ thanh toán COD và ví điện tử. Phí dịch vụ được hiển thị rõ trước khi đặt đơn.\n\n" +
                    "4. Hủy đơn\n" +
                    "Đơn hàng có thể hủy trước khi shopper bắt đầu mua sắm. Tiền sẽ được hoàn vào ví.\n\n" +
                    "5. Trách nhiệm\n" +
                    "GoMarket không chịu trách nhiệm về chất lượng hàng hóa từ chợ/cửa hàng.";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void showHelpDialog() {
        String[] items = {
                "❓  Cách tạo đơn đi chợ hộ",
                "🛒  Cách trở thành Shopper",
                "💰  Cách nạp tiền vào ví",
                "📦  Theo dõi đơn hàng",
                "🏪  Đăng bài Chợ Đồng Hương",
                "🤖  Sử dụng AI Thổ Địa",
                "📞  Liên hệ hỗ trợ"
        };

        new AlertDialog.Builder(this)
                .setTitle("Trợ giúp")
                .setItems(items, (dialog, which) -> {
                    String[] answers = {
                            "🛍️ TẠO ĐƠN ĐI CHỢ HỘ\n\n" +
                            "1. Bấm \"Nhờ đi chợ hộ\" trên trang chủ\n" +
                            "2. Chọn vị trí giao hàng trên bản đồ\n" +
                            "3. Thêm nguyên liệu cần mua (có gợi ý tự động)\n" +
                            "4. Đặt ngân sách và phí shopper\n" +
                            "5. Chọn thanh toán COD hoặc Ví\n" +
                            "6. Xác nhận đơn và chờ shopper nhận",

                            "🏃 TRỞ THÀNH SHOPPER\n\n" +
                            "1. Vào Hồ sơ → Thông tin cá nhân\n" +
                            "2. Vai trò của bạn phải là \"Shopper\"\n" +
                            "3. Bật trạng thái Online trên Dashboard\n" +
                            "4. Các đơn hàng gần bạn (15km) sẽ hiển thị\n" +
                            "5. Nhận đơn và bắt đầu mua sắm",

                            "💳 NẠP TIỀN VÀO VÍ\n\n" +
                            "1. Vào Ví của tôi từ Hồ sơ hoặc trang chủ\n" +
                            "2. Bấm \"Nạp tiền\"\n" +
                            "3. Nhập số tiền cần nạp\n" +
                            "4. Xác nhận giao dịch\n" +
                            "5. Số dư sẽ cập nhật ngay lập tức",

                            "📍 THEO DÕI ĐƠN HÀNG\n\n" +
                            "1. Sau khi đặt đơn, bạn sẽ vào màn hình theo dõi\n" +
                            "2. Trạng thái đơn: Chờ → Đã nhận → Đang mua → Đang giao → Hoàn thành\n" +
                            "3. Xem vị trí shopper trên bản đồ realtime\n" +
                            "4. Chat trực tiếp với shopper trong đơn\n" +
                            "5. Đánh giá sao khi hoàn thành",

                            "🏪 ĐĂNG BÀI CHỢ ĐỒNG HƯƠNG\n\n" +
                            "1. Vào Chợ Đồng Hương từ trang chủ\n" +
                            "2. Bấm nút + để tạo bài mới\n" +
                            "3. Chọn danh mục (nông sản, đặc sản, gom chung...)\n" +
                            "4. Chọn vùng miền và tỉnh thành\n" +
                            "5. Thêm ảnh và nội dung\n" +
                            "6. Đăng bài để mọi người thấy",

                            "🤖 SỬ DỤNG AI THỔ ĐỊA\n\n" +
                            "AI Thổ Địa gợi ý đặc sản theo:\n" +
                            "• Vị trí hiện tại của bạn\n" +
                            "• Thời tiết và mùa vụ\n" +
                            "• Khẩu vị cá nhân\n\n" +
                            "Gợi ý hiển thị trên trang chủ. Bấm vào chip để xem chi tiết và bài đăng liên quan.",

                            "📞 LIÊN HỆ HỖ TRỢ\n\n" +
                            "📧 Email: support@gomarket.vn\n" +
                            "📱 Hotline: 1900 xxxx\n" +
                            "🕐 Giờ làm việc: 8:00 - 22:00\n\n" +
                            "Hoặc gửi phản hồi qua mục Cài đặt trong ứng dụng."
                    };

                    new AlertDialog.Builder(this)
                            .setTitle(items[which].substring(4).trim())
                            .setMessage(answers[which])
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                })
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void showPersonalInfoDialog() {
        String name = sessionManager.getUserName();
        String phone = sessionManager.getUserPhone();
        String email = sessionManager.getUserEmail();
        String role = sessionManager.getUserRole();

        String roleName = "SHOPPER".equals(role) ? "Người đi chợ" : "Người mua";

        StringBuilder info = new StringBuilder();
        info.append("👤  Họ tên: ").append(name.isEmpty() ? "Chưa cập nhật" : name).append("\n\n");
        info.append("📱  Số điện thoại: ").append(phone.isEmpty() ? "Chưa cập nhật" : phone).append("\n\n");
        info.append("📧  Email: ").append(email == null || email.isEmpty() ? "Chưa cập nhật" : email).append("\n\n");
        info.append("🏷️  Vai trò: ").append(roleName);

        new AlertDialog.Builder(this)
                .setTitle("Thông tin cá nhân")
                .setMessage(info.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }
}
