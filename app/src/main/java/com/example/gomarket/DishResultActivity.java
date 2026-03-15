package com.example.gomarket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.adapter.DishAdapter;
import com.example.gomarket.model.RecipeModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DishResultActivity extends AppCompatActivity {

    private RecyclerView rvDishResults;
    private TextView tvNoResult;
    private ProgressBar progressBar;
    private DishAdapter adapter;
    private List<RecipeModel> recipeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_result);

        View btnBackWrap = findViewById(R.id.btnBackWrap);
        if (btnBackWrap != null) btnBackWrap.setOnClickListener(v -> finish());

        rvDishResults = findViewById(R.id.rvDishResults);
        tvNoResult = findViewById(R.id.tvNoResult);
        progressBar = findViewById(R.id.progressBar);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);

        // Get Input from Search Activity
        ArrayList<String> userIngredients = getIntent().getStringArrayListExtra("USER_INGREDIENTS");
        if (userIngredients == null) {
            userIngredients = new ArrayList<>();
        }

        final String searchQuery = userIngredients.isEmpty() ? "" : userIngredients.get(0);
        if (tvSubtitle != null) {
            tvSubtitle.setText(searchQuery.isEmpty() ? "Nhập từ khóa để xem gợi ý" : "Đang tải...");
        }

        rvDishResults.setLayoutManager(new LinearLayoutManager(this));

        // Simulate Network Loading
        progressBar.setVisibility(View.VISIBLE);
        rvDishResults.setVisibility(View.GONE);

        ArrayList<String> finalUserIngredients = userIngredients;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadMockData(finalUserIngredients);
        }, 1500); // 1.5s delay to simulate search
    }

    private void loadMockData(List<String> userIngredients) {
        recipeList = new ArrayList<>();

        // ============ BÚN BÒ ============
        recipeList.add(new RecipeModel("r1", "Bún Bò Huế", "https://example.com/bun_bo_hue.jpg",
                Arrays.asList("Bún", "Bò", "Chả", "Hành", "Rau mùi"), 30));
        recipeList.add(new RecipeModel("r2", "Bún Bò Nam Bộ", "https://example.com/bun_bo_nam_bo.jpg",
                Arrays.asList("Bún", "Bò", "Rau muống", "Đậu phộng"), 25));
        recipeList.add(new RecipeModel("r3", "Bún Riêu Cua", "https://example.com/bun_rieu.jpg",
                Arrays.asList("Bún", "Cua", "Ghẹ", "Tom", "Hành"), 35));

        // ============ CƠM TẤM ============
        recipeList.add(new RecipeModel("r4", "Cơm Tấm Sườn Nướng", "https://example.com/com_tam.jpg",
                Arrays.asList("Cơm", "Sườn", "Chả", "Trứng", "Nước mắm"), 40));
        recipeList.add(new RecipeModel("r5", "Cơm Tấm Bì Chả", "https://example.com/com_tam_bi.jpg",
                Arrays.asList("Cơm", "Bì", "Chả", "Trứng", "Nước sốt"), 35));
        recipeList.add(new RecipeModel("r6", "Cơm Gà Xối Mỡ", "https://example.com/com_ga.jpg",
                Arrays.asList("Cơm", "Gà", "Hành phi", "Nước mỡ"), 38));

        // ============ BÁNH MÌ ============
        recipeList.add(new RecipeModel("r7", "Bánh Mì Thịt Nướng", "https://example.com/banh_mi.jpg",
                Arrays.asList("Bánh mì", "Thịt nướng", "Pate", "Rau", "Nước sốt"), 20));
        recipeList.add(new RecipeModel("r8", "Bánh Mì Chả Lụa", "https://example.com/banh_mi_cha.jpg",
                Arrays.asList("Bánh mì", "Chả lụa", "Pate", "Xà lách"), 18));
        recipeList.add(new RecipeModel("r9", "Bánh Mì Bơ Tỏi", "https://example.com/banh_mi_bo_toi.jpg",
                Arrays.asList("Bánh mì", "Bơ", "Tỏi", "Phô mai"), 15));

        // ============ TRÀ SỮA ============
        recipeList.add(new RecipeModel("r10", "Trà Sữa Trân Châu", "https://example.com/tra_sua.jpg",
                Arrays.asList("Trà", "Sữa", "Trân châu", "Đường phèn"), 25));
        recipeList.add(new RecipeModel("r11", "Trà Sữa Khoai Môn", "https://example.com/tra_sua_khoai.jpg",
                Arrays.asList("Trà", "Sữa", "Khoai môn", "Thạch phô mai"), 28));
        recipeList.add(new RecipeModel("r12", "Trà Đá Bạc Hà", "https://example.com/tra_da.jpg",
                Arrays.asList("Trà", "Đá", "Bạc hà", "Chanh"), 15));

        // ============ THỊT KHO TÀU ============
        recipeList.add(new RecipeModel("r13", "Thịt Kho Tàu", "https://example.com/thit_kho.jpg",
                Arrays.asList("Thịt heo", "Trứng", "Nước dừa", "Hành"), 45));
        recipeList.add(new RecipeModel("r14", "Thịt Kho Tiêu", "https://example.com/thit_kho_tieu.jpg",
                Arrays.asList("Thịt heo", "Tiêu", "Nước mắm", "Hành"), 40));
        recipeList.add(new RecipeModel("r15", "Thịt Kho Trứng Cải Chua", "https://example.com/thit_kho_cai.jpg",
                Arrays.asList("Thịt heo", "Trứng", "Cải chua", "Hành"), 42));

        // ============ MÝ Ý ============
        recipeList.add(new RecipeModel("r16", "Mì Ý Spaghetti", "https://example.com/my_y.jpg",
                Arrays.asList("Mì Ý", "Thịt bò", "Cà chua", "Hành", "Phô mai"), 50));
        recipeList.add(new RecipeModel("r17", "Mì Ý Carbonara", "https://example.com/my_carbonara.jpg",
                Arrays.asList("Mì Ý", "Thịt xông khói", "Trứng", "Phô mai"), 55));
        recipeList.add(new RecipeModel("r18", "Mì Ý Bò Bằm", "https://example.com/my_bo_bam.jpg",
                Arrays.asList("Mì Ý", "Thịt bò", "Cà chua", "Rau mùi"), 48));

        // ============ PHỞ ============
        recipeList.add(new RecipeModel("r19", "Phở Bò Gia Truyền", "https://example.com/pho_bo.jpg",
                Arrays.asList("Bánh phở", "Thịt bò", "Nước dùng", "Hành", "Gia vị"), 45));
        recipeList.add(new RecipeModel("r20", "Phở Bò Tái Nạm", "https://example.com/pho_tai_nam.jpg",
                Arrays.asList("Bánh phở", "Thịt bò", "Tái", "Nạm", "Gia vị"), 50));
        recipeList.add(new RecipeModel("r21", "Phở Gà", "https://example.com/pho_ga.jpg",
                Arrays.asList("Bánh phở", "Thịt gà", "Nước dùng", "Hành"), 40));

        // ============ ĐỒ ĂN NHANH ============
        recipeList.add(new RecipeModel("r22", "Hamburger Bò", "https://example.com/hamburger.jpg",
                Arrays.asList("Bánh mì", "Thịt bò", "Phô mai", "Rau", "Sốt"), 35));
        recipeList.add(new RecipeModel("r23", "KFC Gà Rán", "https://example.com/kfc.jpg",
                Arrays.asList("Gà", "Bột chiên", "Gia vị"), 40));
        recipeList.add(new RecipeModel("r24", "Pizza Hải Sản", "https://example.com/pizza.jpg",
                Arrays.asList("Bánh pizza", "Tôm", "Mực", "Phô mai"), 60));

        // ============ TRÁNG MIỆNG ============
        recipeList.add(new RecipeModel("r25", "Chè Thái", "https://example.com/che_thai.jpg",
                Arrays.asList("Thạch", "Trái cây", "Nước cốt dừa", "Đá"), 20));
        recipeList.add(new RecipeModel("r26", "Bánh Flan", "https://example.com/flan.jpg",
                Arrays.asList("Trứng", "Sữa", "Caramel"), 18));
        recipeList.add(new RecipeModel("r27", "Rau Câu Phô Mai", "https://example.com/rau_cau.jpg",
                Arrays.asList("Rau câu", "Phô mai", "Nước dừa"), 15));

        // ============ ĐỒ UỐNG ============
        recipeList.add(new RecipeModel("r28", "Nước Cam Tươi", "https://example.com/cam.jpg",
                Arrays.asList("Cam", "Đường", "Đá"), 15));
        recipeList.add(new RecipeModel("r29", "Sinh Tố Bơ", "https://example.com/sinh_to_bo.jpg",
                Arrays.asList("Bơ", "Sữa", "Đường phèn", "Đá"), 22));
        recipeList.add(new RecipeModel("r30", "Sữa Chua Đánh Đá", "https://example.com/sua_chua.jpg",
                Arrays.asList("Sữa chua", "Đá", "Đường", "Trái cây"), 18));

        // ============ HEALTHY ============
        recipeList.add(new RecipeModel("r31", "Salad Rau Mầm", "https://example.com/salad.jpg",
                Arrays.asList("Rau mầm", "Cà chua", "Dầu ô liu", "Hạt"), 30));
        recipeList.add(new RecipeModel("r32", "Gà Án Chảo", "https://example.com/ga_an_chao.jpg",
                Arrays.asList("Thịt gà", "Rau xanh", "Gia vị ít năng lượng"), 35));
        recipeList.add(new RecipeModel("r33", "Cơm Gạo Lứt", "https://example.com/com_gao_lut.jpg",
                Arrays.asList("Gạo lứt", "Thịt cá", "Rau"), 32));

        // ============ GÀ ============
        recipeList.add(new RecipeModel("r34", "Gà Xào Cà Chua", "https://example.com/ga_ca_chua.jpg",
                Arrays.asList("Gà", "Cà chua", "Hành"), 25));
        recipeList.add(new RecipeModel("r35", "Canh Gà Lá Giang", "https://example.com/canh_ga.jpg",
                Arrays.asList("Gà", "Lá giang", "Hành", "Ớt"), 40));
        recipeList.add(new RecipeModel("r36", "Gà Nướng Mật Ong", "https://example.com/ga_nuong.jpg",
                Arrays.asList("Gà", "Mật ong", "Tỏi", "Gia vị"), 45));

        // Filter recipes based on ingredients (match if search text is contained in recipe name or ingredients)
        List<RecipeModel> matchedRecipes = new ArrayList<>();
        String searchQuery = userIngredients.isEmpty() ? "" : userIngredients.get(0).toLowerCase();

        for (RecipeModel recipe : recipeList) {
            boolean hasMatch = false;

            // Check recipe name
            if (recipe.getName().toLowerCase().contains(searchQuery)) {
                hasMatch = true;
            }

            // Check ingredients
            if (!hasMatch) {
                for (String req : recipe.getIngredients()) {
                    if (req.toLowerCase().contains(searchQuery)) {
                        hasMatch = true;
                        break;
                    }
                }
            }

            // Also check if ingredient matches recipe name
            if (!hasMatch) {
                for (String user : userIngredients) {
                    String userLower = user.toLowerCase();
                    if (recipe.getName().toLowerCase().contains(userLower)) {
                        hasMatch = true;
                        break;
                    }
                    for (String req : recipe.getIngredients()) {
                        if (req.toLowerCase().contains(userLower)) {
                            hasMatch = true;
                            break;
                        }
                    }
                    if (hasMatch) break;
                }
            }

            if (hasMatch) {
                matchedRecipes.add(recipe);
            }
        }

        // If no match found but there are search terms, show all recipes related to common foods
        if (matchedRecipes.isEmpty() && !searchQuery.isEmpty()) {
            matchedRecipes.addAll(recipeList.subList(0, Math.min(6, recipeList.size())));
        }

        progressBar.setVisibility(View.GONE);

        String q = userIngredients.isEmpty() ? "" : userIngredients.get(0);
        TextView st = findViewById(R.id.tvSubtitle);
        if (st != null) st.setText(matchedRecipes.size() + " món phù hợp với \"" + q + "\"");

        if (matchedRecipes.isEmpty()) {
            tvNoResult.setVisibility(View.VISIBLE);
            rvDishResults.setVisibility(View.GONE);
            View bottomBar = findViewById(R.id.bottomBar);
            if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        } else {
            tvNoResult.setVisibility(View.GONE);
            rvDishResults.setVisibility(View.VISIBLE);
            adapter = new DishAdapter(this, matchedRecipes, userIngredients);
            rvDishResults.setAdapter(adapter);
            TextView tvBottomSummary = findViewById(R.id.tvBottomSummary);
            if (tvBottomSummary != null) tvBottomSummary.setText("Chọn món để nhờ shopper mua nguyên liệu");
        }
    }
}
