# Log thay đổi dự án DiChoHo

## 2026-03-18

### Sửa lỗi build RecipeCardAdapter + HomeActivity

- **RecipeCardAdapter.java**
  - Đổi ID layout cho đúng với `item_recipe_card.xml`: `ivRecipeImage` → `R.id.imgRecipe`, `tvCost` → `R.id.tvEstimatedPrice`.
  - Thêm tham chiếu `tvDescription` → `R.id.tvDescription`.

- **item_recipe_card.xml**
  - Thêm `TextView` với `android:id="@+id/tvDescription"` để adapter hiển thị mô tả món.

- **activity_home.xml**
  - Thêm lại phần Bản tin chợ đồng hương: `tvSeeAllPosts`, `rvCommunityFeed`, `emptyFeedState` để HomeActivity không báo lỗi thiếu view.

### Sửa lỗi build CookbookActivity (View Binding → findViewById)

- **CookbookActivity.java**
  - Xóa dùng View Binding (`ActivityCookbookBinding`), chuyển sang `setContentView(R.layout.activity_cookbook)` và `findViewById` cho `viewPager`, `tabLayout`, `btnBack`, `btnAdd`.
  - Thêm import: `FragmentStateAdapter`, `TabLayout`, bỏ import `com.example.gomarket.databinding.ActivityCookbookBinding`.

- **AndroidManifest.xml**
  - Đăng ký `CookbookActivity`: `<activity android:name=".CookbookActivity" android:exported="false" />`.
