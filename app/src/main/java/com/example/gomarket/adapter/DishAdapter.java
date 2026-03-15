package com.example.gomarket.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        holder.tvCookTime.setText(recipe.getCookTime() + " phút nấu");

        // Tag: Phổ biến / Dễ làm (luân phiên theo vị trí)
        holder.tvTag.setVisibility(View.VISIBLE);
        holder.tvTag.setText(position % 2 == 0 ? "Phổ biến" : "Dễ làm");

        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.dish_placeholder)
                .error(R.drawable.dish_placeholder)
                .centerCrop()
                .into(holder.ivDishImage);

        // Tính nguyên liệu đã có / thiếu
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
            if (!found) missing.add(reqIngredient);
        }

        int total = recipe.getIngredients().size();
        int haveCount = total - missing.size();

        if (missing.isEmpty()) {
            holder.tvIngredientStatus.setText("Đã có đủ " + total + "/" + total + " nguyên liệu");
            holder.tvIngredientStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark));
            holder.icIngredientStatus.setVisibility(View.VISIBLE);
            holder.tvCookNow.setVisibility(View.VISIBLE);
            holder.progressIngredients.setVisibility(View.GONE);
            holder.tvMissingHint.setVisibility(View.GONE);
            holder.btnAskShopper.setVisibility(View.GONE);
        } else {
            holder.tvIngredientStatus.setText("Có " + haveCount + "/" + total);
            holder.tvIngredientStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark));
            holder.icIngredientStatus.setVisibility(View.VISIBLE);
            holder.tvCookNow.setVisibility(View.GONE);
            holder.progressIngredients.setVisibility(View.VISIBLE);
            holder.progressIngredients.setProgress(total > 0 ? (haveCount * 100 / total) : 0);
            holder.tvMissingHint.setVisibility(View.VISIBLE);
            holder.tvMissingHint.setText("+ Cần mua " + missing.size());
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
        ImageView ivDishImage, icIngredientStatus;
        TextView tvDishName, tvCookTime, tvTag, tvIngredientStatus, tvCookNow, tvMissingHint;
        ProgressBar progressIngredients;
        View btnAskShopper;

        public DishViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDishImage = itemView.findViewById(R.id.ivDishImage);
            tvDishName = itemView.findViewById(R.id.tvDishName);
            tvCookTime = itemView.findViewById(R.id.tvCookTime);
            tvTag = itemView.findViewById(R.id.tvTag);
            icIngredientStatus = itemView.findViewById(R.id.icIngredientStatus);
            tvIngredientStatus = itemView.findViewById(R.id.tvIngredientStatus);
            tvCookNow = itemView.findViewById(R.id.tvCookNow);
            progressIngredients = itemView.findViewById(R.id.progressIngredients);
            tvMissingHint = itemView.findViewById(R.id.tvMissingHint);
            btnAskShopper = itemView.findViewById(R.id.btnAskShopper);
        }
    }
}
