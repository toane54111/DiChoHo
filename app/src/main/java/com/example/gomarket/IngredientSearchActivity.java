package com.example.gomarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IngredientSearchActivity extends AppCompatActivity {

    private EditText etIngredientInput;
    private Button btnSearchDish;
    private RecyclerView rvIngredientSuggestion;
    
    private List<String> suggestionList = Arrays.asList("Gà", "Trứng", "Cà chua", "Hành", "Khoai tây", "Thịt bò", "Hành tây", "Tiêu", "Cà rốt");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_search);

        etIngredientInput = findViewById(R.id.etIngredientInput);
        btnSearchDish = findViewById(R.id.btnSearchDish);
        rvIngredientSuggestion = findViewById(R.id.rvIngredientSuggestion);

        // Setup Suggestion RecyclerView
        rvIngredientSuggestion.setLayoutManager(new GridLayoutManager(this, 3));
        rvIngredientSuggestion.setAdapter(new SuggestionAdapter(suggestionList));

        btnSearchDish.setOnClickListener(v -> {
            String input = etIngredientInput.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nguyên liệu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Split and clean input
            String[] ingredients = input.split(",");
            ArrayList<String> ingredientList = new ArrayList<>();
            for (String ing : ingredients) {
                if (!ing.trim().isEmpty()) {
                    ingredientList.add(ing.trim());
                }
            }

            // Move to results
            Intent intent = new Intent(IngredientSearchActivity.this, DishResultActivity.class);
            intent.putStringArrayListExtra("USER_INGREDIENTS", ingredientList);
            startActivity(intent);
        });
    }

    // Simple inner adapter for tags
    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {
        private List<String> list;

        public SuggestionAdapter(List<String> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient_suggestion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = list.get(position);
            holder.tvIngredientName.setText(item);
            holder.itemView.setOnClickListener(v -> {
                String current = etIngredientInput.getText().toString().trim();
                if (current.isEmpty()) {
                    etIngredientInput.setText(item);
                } else {
                    etIngredientInput.setText(current + ", " + item);
                }
                etIngredientInput.setSelection(etIngredientInput.getText().length());
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIngredientName;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            }
        }
    }
}
