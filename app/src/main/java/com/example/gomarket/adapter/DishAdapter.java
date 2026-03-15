package com.example.gomarket.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.R;
import com.example.gomarket.SelectShopperActivity;
import com.example.gomarket.model.RecipeModel;

import java.util.ArrayList;
import java.util.List;

public class DishAdapter extends RecyclerView.Adapter<DishAdapter.DishViewHolder> {

    private Context context;
    private List<RecipeModel> recipeList;
    private List<String> userIngredients;

    public DishAdapter(Context context, List<RecipeModel> recipeList, List<String> userIngredients) {
        this.context = context;
        this.recipeList = recipeList;
        this.userIngredients = userIngredients;
    }

    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dish, parent, false);
        return new DishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DishViewHolder holder, int position) {
        RecipeModel recipe = recipeList.get(position);

        holder.tvDishName.setText(recipe.getName());
        holder.tvCookTime.setText("Thời gian nấu: " + recipe.getCookTime() + " phút");

        // Load image (using placeholder for now)
        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.dish_placeholder)
                .error(R.drawable.dish_placeholder)
                .into(holder.ivDishImage);

        // Format ingredients
        String allIngredients = String.join(", ", recipe.getIngredients());
        holder.tvIngredients.setText(allIngredients);

        // Calculate missing ingredients
        List<String> missing = new ArrayList<>();
        for (String reqIngredient : recipe.getIngredients()) {
            boolean found = false;
            for (String userIng : userIngredients) {
                if (reqIngredient.toLowerCase().contains(userIng.toLowerCase()) || 
                    userIng.toLowerCase().contains(reqIngredient.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missing.add(reqIngredient);
            }
        }

        if (missing.isEmpty()) {
            holder.tvMissingIngredients.setText("Không thiếu gì cả!");
            holder.tvMissingIngredients.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.btnAskShopper.setVisibility(View.GONE);
        } else {
            holder.tvMissingIngredients.setText(String.join(", ", missing));
            holder.tvMissingIngredients.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.btnAskShopper.setVisibility(View.VISIBLE);
        }

        holder.btnAskShopper.setOnClickListener(v -> {
            Intent intent = new Intent(context, SelectShopperActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getRecipeId());
            intent.putStringArrayListExtra("MISSING_INGREDIENTS", new ArrayList<>(missing));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public static class DishViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDishImage;
        TextView tvDishName, tvIngredients, tvMissingIngredients, tvCookTime;
        Button btnAskShopper;

        public DishViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDishImage = itemView.findViewById(R.id.ivDishImage);
            tvDishName = itemView.findViewById(R.id.tvDishName);
            tvIngredients = itemView.findViewById(R.id.tvIngredients);
            tvMissingIngredients = itemView.findViewById(R.id.tvMissingIngredients);
            tvCookTime = itemView.findViewById(R.id.tvCookTime);
            btnAskShopper = itemView.findViewById(R.id.btnAskShopper);
        }
    }
}
