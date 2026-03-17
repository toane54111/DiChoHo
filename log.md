# Log thay đổi dự án DiChoHo

## 2026-03-18

### Sửa lỗi build CookbookActivity (View Binding → findViewById)

- **CookbookActivity.java**
  - Xóa dùng View Binding (`ActivityCookbookBinding`), chuyển sang `setContentView(R.layout.activity_cookbook)` và `findViewById` cho `viewPager`, `tabLayout`, `btnBack`, `btnAdd`.
  - Thêm import: `FragmentStateAdapter`, `TabLayout`, bỏ import `com.example.gomarket.databinding.ActivityCookbookBinding`.

- **AndroidManifest.xml**
  - Đăng ký `CookbookActivity`: `<activity android:name=".CookbookActivity" android:exported="false" />`.
