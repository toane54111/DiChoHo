package com.example.gomarket.adapter;

import android.graphics.Typeface;
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
        boolean hasMatch = ingredient.getMatchedProduct() != null;

        holder.tvIcon.setText(FOOD_ICONS[position % FOOD_ICONS.length]);
        holder.tvIngredientName.setText(ingredient.getName());
        holder.tvIngredientQuantity.setText(ingredient.getQuantity());

        if (hasMatch) {
            // Còn hàng - hiển thị giá
            holder.tvIngredientPrice.setText(ingredient.getMatchedProduct().getFormattedPrice());
            holder.tvIngredientPrice.setTextColor(0xFF4CAF50); // primary green
            holder.tvIngredientPrice.setTypeface(null, Typeface.BOLD);
            holder.tvIngredientPrice.setVisibility(View.VISIBLE);

            // Style bình thường
            holder.tvIngredientName.setTextColor(0xFF212121);
            holder.tvIngredientName.setTypeface(null, Typeface.NORMAL);
            holder.itemView.setAlpha(1.0f);
        } else {
            // Hết hàng - hiển thị label "Hết hàng"
            holder.tvIngredientPrice.setText("Hết hàng");
            holder.tvIngredientPrice.setTextColor(0xFFE53935); // red
            holder.tvIngredientPrice.setTypeface(null, Typeface.ITALIC);
            holder.tvIngredientPrice.setVisibility(View.VISIBLE);

            // Dim row để thể hiện không khả dụng
            holder.tvIngredientName.setTextColor(0xFF9E9E9E);
            holder.tvIngredientName.setTypeface(null, Typeface.ITALIC);
            holder.itemView.setAlpha(0.6f);
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
