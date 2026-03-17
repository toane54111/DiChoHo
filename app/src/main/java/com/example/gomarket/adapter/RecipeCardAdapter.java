package com.example.gomarket.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.Recipe;

import java.util.List;

public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.RecipeViewHolder> {

    private Context context;
    private List<Recipe> recipes;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    public RecipeCardAdapter(Context context, List<Recipe> recipes, OnRecipeClickListener listener) {
        this.context = context;
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe_card, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.tvRecipeName.setText(recipe.getName());
        holder.tvDescription.setText(recipe.getDescription());
        holder.tvCost.setText(recipe.getFormattedTotalCost());
        holder.tvLikeCount.setText(String.valueOf((int) (Math.random() * 500)));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName, tvDescription, tvCost, tvLikeCount;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.imgRecipe);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCost = itemView.findViewById(R.id.tvEstimatedPrice);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
        }
    }
}
