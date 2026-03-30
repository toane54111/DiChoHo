package com.example.gomarket.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.R;
import com.example.gomarket.model.CookbookRecipe;
import com.example.gomarket.network.ApiClient;

import java.util.List;

public class CookbookRecipeAdapter extends RecyclerView.Adapter<CookbookRecipeAdapter.ViewHolder> {

    public interface OnRecipeActionListener {
        void onRecipeClick(CookbookRecipe recipe);
        void onLikeClick(CookbookRecipe recipe, int position);
    }

    private final Context context;
    private List<CookbookRecipe> recipes;
    private final OnRecipeActionListener listener;

    public CookbookRecipeAdapter(Context context, List<CookbookRecipe> recipes, OnRecipeActionListener listener) {
        this.context = context;
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cookbook_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookbookRecipe recipe = recipes.get(position);

        // Load image
        String imageUrl = recipe.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivRecipeImage.setVisibility(View.VISIBLE);
            String fullUrl = ApiClient.getFullImageUrl(imageUrl);
            Glide.with(context).load(fullUrl).centerCrop().into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvDescription.setText(recipe.getDescription());
        holder.tvCost.setText(recipe.getFormattedTotalCost());
        holder.tvAuthor.setText(recipe.isSystemRecipe() ? "📖 GoMarket" : "👤 " + recipe.getAuthorName());

        // Like state
        holder.tvLikeCount.setText(recipe.getLikeCount() > 0 ?
                String.valueOf(recipe.getLikeCount()) : "");
        holder.tvLikeIcon.setText(recipe.isLikedByUser() ? "♥" : "♡");
        holder.tvLikeIcon.setTextColor(context.getResources().getColor(
                recipe.isLikedByUser() ? android.R.color.holo_red_light : android.R.color.darker_gray));

        // Comment count
        holder.tvCommentCount.setText(recipe.getCommentCount() > 0 ?
                String.valueOf(recipe.getCommentCount()) : "");

        // Click handlers
        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
        holder.layoutLike.setOnClickListener(v -> listener.onLikeClick(recipe, position));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateRecipes(List<CookbookRecipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvCost, tvAuthor;
        TextView tvLikeIcon, tvLikeCount, tvCommentCount;
        ImageView ivRecipeImage;
        View layoutLike;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeImage = itemView.findViewById(R.id.ivRecipeImage);
            tvTitle = itemView.findViewById(R.id.tvRecipeTitle);
            tvDescription = itemView.findViewById(R.id.tvRecipeDesc);
            tvCost = itemView.findViewById(R.id.tvRecipeCost);
            tvAuthor = itemView.findViewById(R.id.tvRecipeAuthor);
            tvLikeIcon = itemView.findViewById(R.id.tvLikeIcon);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            layoutLike = itemView.findViewById(R.id.layoutLike);
        }
    }
}
