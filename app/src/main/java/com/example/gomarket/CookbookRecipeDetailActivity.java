package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.example.gomarket.model.CookbookComment;
import com.example.gomarket.model.CookbookRecipe;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CookbookRecipeDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor, tvDescription, tvCost;
    private TextView tvLikeIcon, tvLikeCount, tvCommentTitle;
    private LinearLayout layoutIngredients, layoutSteps, layoutComments, contentLayout;
    private View btnLike, btnAddToCart, btnSendComment, cardImage;
    private ImageView ivRecipeImage;
    private EditText etComment;
    private ProgressBar progressBar;

    private ApiService apiService;
    private SessionManager session;
    private CookbookRecipe recipe;
    private long recipeId;
    private Gson gson = new Gson();

    // Parsed ingredients for shopping request
    private List<Map<String, Object>> ingredientsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookbook_recipe_detail);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);
        recipeId = getIntent().getLongExtra("RECIPE_ID", -1);

        initViews();
        loadRecipe();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvCost = findViewById(R.id.tvCost);
        tvLikeIcon = findViewById(R.id.tvLikeIcon);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentTitle = findViewById(R.id.tvCommentTitle);
        layoutIngredients = findViewById(R.id.layoutIngredients);
        layoutSteps = findViewById(R.id.layoutSteps);
        layoutComments = findViewById(R.id.layoutComments);
        contentLayout = findViewById(R.id.contentLayout);
        btnLike = findViewById(R.id.btnLike);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnSendComment = findViewById(R.id.btnSendComment);
        etComment = findViewById(R.id.etComment);
        progressBar = findViewById(R.id.progressBar);
        cardImage = findViewById(R.id.cardImage);
        ivRecipeImage = findViewById(R.id.ivRecipeImage);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnLike.setOnClickListener(v -> toggleLike());
        btnAddToCart.setOnClickListener(v -> addToShoppingRequest());
        btnSendComment.setOnClickListener(v -> sendComment());
    }

    private void loadRecipe() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        long userId = session.getUserId();
        apiService.getCookbookRecipe(recipeId, userId > 0 ? userId : null)
                .enqueue(new Callback<CookbookRecipe>() {
                    @Override
                    public void onResponse(Call<CookbookRecipe> call, Response<CookbookRecipe> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            recipe = response.body();
                            displayRecipe();
                            loadComments();
                        } else {
                            Toast.makeText(CookbookRecipeDetailActivity.this,
                                    "Không tải được công thức", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<CookbookRecipe> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CookbookRecipeDetailActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayRecipe() {
        contentLayout.setVisibility(View.VISIBLE);

        // Load recipe image
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            cardImage.setVisibility(View.VISIBLE);
            String fullUrl = ApiClient.getFullImageUrl(imageUrl);
            Glide.with(this).load(fullUrl).centerCrop().into(ivRecipeImage);
        } else {
            cardImage.setVisibility(View.GONE);
        }

        tvTitle.setText(recipe.getTitle());
        tvAuthor.setText(recipe.isSystemRecipe() ? "📖 GoMarket" : "👤 " + recipe.getAuthorName());
        tvDescription.setText(recipe.getDescription());
        tvCost.setText(recipe.getFormattedTotalCost());
        updateLikeUI();

        // Parse and display ingredients
        layoutIngredients.removeAllViews();
        ingredientsList = new ArrayList<>();
        try {
            List<Map<String, Object>> ingredients = gson.fromJson(recipe.getIngredientsJson(),
                    new TypeToken<List<Map<String, Object>>>() {}.getType());
            if (ingredients != null) {
                ingredientsList = ingredients;
                for (Map<String, Object> ing : ingredients) {
                    addIngredientRow(
                            (String) ing.get("name"),
                            (String) ing.get("quantity"),
                            ing.get("estimated_price") != null ? ((Number) ing.get("estimated_price")).doubleValue() : 0
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Parse and display steps
        layoutSteps.removeAllViews();
        try {
            List<String> steps = gson.fromJson(recipe.getStepsJson(),
                    new TypeToken<List<String>>() {}.getType());
            if (steps != null) {
                for (int i = 0; i < steps.size(); i++) {
                    addStepRow(i + 1, steps.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addIngredientRow(String name, String quantity, double price) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));
        tvName.setText("• " + name);
        tvName.setTextSize(14);
        tvName.setTextColor(0xFF212121);

        TextView tvQty = new TextView(this);
        tvQty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        tvQty.setText(quantity);
        tvQty.setTextSize(13);
        tvQty.setTextColor(0xFF757575);

        TextView tvPrice = new TextView(this);
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        tvPrice.setText(price > 0 ? String.format("~%,.0fđ", price) : "");
        tvPrice.setTextSize(13);
        tvPrice.setTextColor(0xFF4CAF50);
        tvPrice.setGravity(android.view.Gravity.END);

        row.addView(tvName);
        row.addView(tvQty);
        row.addView(tvPrice);
        layoutIngredients.addView(row);
    }

    private void addStepRow(int stepNum, String stepText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvNum = new TextView(this);
        tvNum.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvNum.setText("Bước " + stepNum + ": ");
        tvNum.setTextSize(14);
        tvNum.setTextColor(0xFFFF9800);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvText = new TextView(this);
        tvText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvText.setText(stepText);
        tvText.setTextSize(14);
        tvText.setTextColor(0xFF424242);

        row.addView(tvNum);
        row.addView(tvText);
        layoutSteps.addView(row);
    }

    private void updateLikeUI() {
        tvLikeIcon.setText(recipe.isLikedByUser() ? "♥" : "♡");
        tvLikeIcon.setTextColor(recipe.isLikedByUser() ? 0xFFE53935 : 0xFF9E9E9E);
        tvLikeCount.setText(recipe.getLikeCount() > 0 ? String.valueOf(recipe.getLikeCount()) : "");
    }

    private void toggleLike() {
        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.toggleCookbookLike(recipeId, userId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                            int count = ((Number) response.body().get("likeCount")).intValue();
                            recipe.setIsLikedByUser(liked);
                            recipe.setLikeCount(count);
                            updateLikeUI();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
    }

    private void addToShoppingRequest() {
        if (ingredientsList == null || ingredientsList.isEmpty()) {
            Toast.makeText(this, "Không có nguyên liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CreateShoppingRequestActivity.class);
        intent.putExtra("fromAIChef", true);

        ArrayList<String> itemNames = new ArrayList<>();
        ArrayList<String> itemQuantities = new ArrayList<>();
        double[] itemPrices = new double[ingredientsList.size()];
        double totalBudget = 0;

        for (int i = 0; i < ingredientsList.size(); i++) {
            Map<String, Object> ing = ingredientsList.get(i);
            itemNames.add((String) ing.get("name"));
            itemQuantities.add((String) ing.get("quantity"));
            double price = ing.get("estimated_price") != null ?
                    ((Number) ing.get("estimated_price")).doubleValue() : 0;
            itemPrices[i] = price;
            totalBudget += price;
        }

        intent.putStringArrayListExtra("itemNames", itemNames);
        intent.putStringArrayListExtra("itemQuantities", itemQuantities);
        intent.putExtra("itemPrices", itemPrices);
        intent.putExtra("budget", totalBudget);
        startActivity(intent);
    }

    // ═══ COMMENTS ═══

    private void loadComments() {
        apiService.getCookbookComments(recipeId).enqueue(new Callback<List<CookbookComment>>() {
            @Override
            public void onResponse(Call<List<CookbookComment>> call,
                                   Response<List<CookbookComment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayComments(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CookbookComment>> call, Throwable t) {}
        });
    }

    private void displayComments(List<CookbookComment> comments) {
        layoutComments.removeAllViews();
        tvCommentTitle.setText("Bình luận (" + comments.size() + ")");

        if (comments.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Chưa có bình luận nào");
            tv.setTextColor(0xFF9E9E9E);
            tv.setTextSize(13);
            tv.setPadding(0, 8, 0, 8);
            layoutComments.addView(tv);
            return;
        }

        for (CookbookComment comment : comments) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 12);

            TextView tvAuthor = new TextView(this);
            tvAuthor.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Người dùng");
            tvAuthor.setTextSize(13);
            tvAuthor.setTextColor(0xFF4CAF50);
            tvAuthor.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvContent = new TextView(this);
            tvContent.setText(comment.getContent());
            tvContent.setTextSize(14);
            tvContent.setTextColor(0xFF424242);
            tvContent.setPadding(0, 4, 0, 0);

            row.addView(tvAuthor);
            row.addView(tvContent);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFE0E0E0);

            layoutComments.addView(row);
            layoutComments.addView(divider);
        }
    }

    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) return;

        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("content", content);

        etComment.setText("");

        apiService.addCookbookComment(recipeId, body).enqueue(new Callback<CookbookComment>() {
            @Override
            public void onResponse(Call<CookbookComment> call, Response<CookbookComment> response) {
                if (response.isSuccessful()) {
                    loadComments();
                    Toast.makeText(CookbookRecipeDetailActivity.this,
                            "Đã bình luận", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CookbookComment> call, Throwable t) {
                Toast.makeText(CookbookRecipeDetailActivity.this,
                        "Lỗi gửi bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
