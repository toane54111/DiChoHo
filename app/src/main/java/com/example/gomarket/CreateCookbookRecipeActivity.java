package com.example.gomarket;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gomarket.model.CookbookRecipe;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateCookbookRecipeActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private LinearLayout containerIngredients, containerSteps;
    private List<View> ingredientRows = new ArrayList<>();
    private List<View> stepRows = new ArrayList<>();
    private ApiService apiService;
    private SessionManager session;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_cookbook_recipe);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        containerIngredients = findViewById(R.id.containerIngredients);
        containerSteps = findViewById(R.id.containerSteps);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddIngredient).setOnClickListener(v -> addIngredientRow("", "", ""));
        findViewById(R.id.btnAddStep).setOnClickListener(v -> addStepRow(""));
        findViewById(R.id.btnSubmit).setOnClickListener(v -> submitRecipe());

        // Default 3 ingredient rows + 3 step rows
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");
        addStepRow("");
        addStepRow("");
        addStepRow("");
    }

    private void addIngredientRow(String name, String qty, String price) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);

        EditText etName = new EditText(this);
        etName.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 3));
        etName.setHint("Tên NL");
        etName.setText(name);
        etName.setTextSize(13);
        etName.setBackground(getDrawable(R.drawable.bg_search_bar));
        etName.setPadding(dp(8), 0, dp(4), 0);
        etName.setTag("name");

        EditText etQty = new EditText(this);
        etQty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 2));
        etQty.setHint("SL");
        etQty.setText(qty);
        etQty.setTextSize(13);
        etQty.setBackground(getDrawable(R.drawable.bg_search_bar));
        etQty.setPadding(dp(8), 0, dp(4), 0);
        LinearLayout.LayoutParams qtyParams = (LinearLayout.LayoutParams) etQty.getLayoutParams();
        qtyParams.setMarginStart(dp(4));
        etQty.setTag("qty");

        EditText etPrice = new EditText(this);
        etPrice.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 2));
        etPrice.setHint("Giá");
        etPrice.setText(price);
        etPrice.setTextSize(13);
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPrice.setBackground(getDrawable(R.drawable.bg_search_bar));
        etPrice.setPadding(dp(8), 0, dp(4), 0);
        LinearLayout.LayoutParams priceParams = (LinearLayout.LayoutParams) etPrice.getLayoutParams();
        priceParams.setMarginStart(dp(4));
        etPrice.setTag("price");

        // Remove button
        TextView btnRemove = new TextView(this);
        btnRemove.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(44)));
        btnRemove.setText("✕");
        btnRemove.setGravity(android.view.Gravity.CENTER);
        btnRemove.setTextColor(0xFFE53935);
        btnRemove.setTextSize(16);
        btnRemove.setOnClickListener(v -> {
            if (ingredientRows.size() > 1) {
                containerIngredients.removeView(row);
                ingredientRows.remove(row);
            }
        });

        row.addView(etName);
        row.addView(etQty);
        row.addView(etPrice);
        row.addView(btnRemove);
        containerIngredients.addView(row);
        ingredientRows.add(row);
    }

    private void addStepRow(String stepText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);

        int stepNum = stepRows.size() + 1;
        TextView tvNum = new TextView(this);
        tvNum.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(44)));
        tvNum.setText("B" + stepNum);
        tvNum.setGravity(android.view.Gravity.CENTER);
        tvNum.setTextColor(0xFFFF9800);
        tvNum.setTextSize(13);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);

        EditText etStep = new EditText(this);
        etStep.setLayoutParams(new LinearLayout.LayoutParams(0, dp(44), 1));
        etStep.setHint("Mô tả bước " + stepNum);
        etStep.setText(stepText);
        etStep.setTextSize(13);
        etStep.setBackground(getDrawable(R.drawable.bg_search_bar));
        etStep.setPadding(dp(8), 0, dp(4), 0);
        etStep.setTag("step");

        TextView btnRemove = new TextView(this);
        btnRemove.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(44)));
        btnRemove.setText("✕");
        btnRemove.setGravity(android.view.Gravity.CENTER);
        btnRemove.setTextColor(0xFFE53935);
        btnRemove.setTextSize(16);
        btnRemove.setOnClickListener(v -> {
            if (stepRows.size() > 1) {
                containerSteps.removeView(row);
                stepRows.remove(row);
                renumberSteps();
            }
        });

        row.addView(tvNum);
        row.addView(etStep);
        row.addView(btnRemove);
        containerSteps.addView(row);
        stepRows.add(row);
    }

    private void renumberSteps() {
        for (int i = 0; i < stepRows.size(); i++) {
            LinearLayout row = (LinearLayout) stepRows.get(i);
            TextView tvNum = (TextView) row.getChildAt(0);
            tvNum.setText("B" + (i + 1));
            EditText etStep = (EditText) row.getChildAt(1);
            etStep.setHint("Mô tả bước " + (i + 1));
        }
    }

    private void submitRecipe() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect ingredients
        List<Map<String, Object>> ingredients = new ArrayList<>();
        double totalCost = 0;
        for (View row : ingredientRows) {
            LinearLayout lr = (LinearLayout) row;
            String name = ((EditText) lr.findViewWithTag("name")).getText().toString().trim();
            String qty = ((EditText) lr.findViewWithTag("qty")).getText().toString().trim();
            String priceStr = ((EditText) lr.findViewWithTag("price")).getText().toString().trim();
            if (name.isEmpty()) continue;

            Map<String, Object> ing = new HashMap<>();
            ing.put("name", name);
            ing.put("quantity", qty.isEmpty() ? "vừa đủ" : qty);
            double price = 0;
            try { price = Double.parseDouble(priceStr); } catch (NumberFormatException ignored) {}
            ing.put("estimated_price", price);
            totalCost += price;
            ingredients.add(ing);
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất 1 nguyên liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect steps
        List<String> steps = new ArrayList<>();
        for (View row : stepRows) {
            LinearLayout lr = (LinearLayout) row;
            String step = ((EditText) lr.findViewWithTag("step")).getText().toString().trim();
            if (!step.isEmpty()) steps.add(step);
        }

        if (steps.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất 1 bước", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("title", title);
        body.put("description", etDescription.getText().toString().trim());
        body.put("ingredientsJson", gson.toJson(ingredients));
        body.put("stepsJson", gson.toJson(steps));
        body.put("totalCost", totalCost);
        body.put("isSystemRecipe", false);

        findViewById(R.id.btnSubmit).setEnabled(false);

        apiService.createCookbookRecipe(body).enqueue(new Callback<CookbookRecipe>() {
            @Override
            public void onResponse(Call<CookbookRecipe> call, Response<CookbookRecipe> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateCookbookRecipeActivity.this,
                            "Đã chia sẻ công thức!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateCookbookRecipeActivity.this,
                            "Lỗi tạo công thức", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.btnSubmit).setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<CookbookRecipe> call, Throwable t) {
                Toast.makeText(CreateCookbookRecipeActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                findViewById(R.id.btnSubmit).setEnabled(true);
            }
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
