package com.example.gomarket.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.Recipe;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {

    private List<Recipe.Ingredient> ingredients;

    private static final String[] FOOD_ICONS = {
            "🥩", "🍄", "🦐", "🥬", "🧅", "🌶", "🥕", "🐟", "🥚", "🧄"
    };

    public IngredientAdapter(List<Recipe.Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Recipe.Ingredient ingredient = ingredients.get(position);

        holder.tvIcon.setText(FOOD_ICONS[position % FOOD_ICONS.length]);
        holder.tvIngredientName.setText(ingredient.getName());
        holder.tvIngredientQuantity.setText(ingredient.getQuantity());

        if (ingredient.getMatchedProduct() != null) {
            holder.tvIngredientPrice.setText(ingredient.getMatchedProduct().getFormattedPrice());
            holder.tvIngredientPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvIngredientPrice.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvIngredientName, tvIngredientQuantity, tvIngredientPrice;

        IngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvIngredientQuantity = itemView.findViewById(R.id.tvIngredientQuantity);
            tvIngredientPrice = itemView.findViewById(R.id.tvIngredientPrice);
        }
    }
}
