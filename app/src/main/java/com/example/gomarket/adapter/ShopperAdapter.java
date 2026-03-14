package com.example.gomarket.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.ChatActivity;
import com.example.gomarket.R;
import com.example.gomarket.model.ShopperModel;

import java.util.List;

public class ShopperAdapter extends RecyclerView.Adapter<ShopperAdapter.ShopperViewHolder> {

    private Context context;
    private List<ShopperModel> shopperList;
    private String recipeId;
    private List<String> missingIngredients;

    public ShopperAdapter(Context context, List<ShopperModel> shopperList, String recipeId, List<String> missingIngredients) {
        this.context = context;
        this.shopperList = shopperList;
        this.recipeId = recipeId;
        this.missingIngredients = missingIngredients;
    }

    @NonNull
    @Override
    public ShopperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopper, parent, false);
        return new ShopperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopperViewHolder holder, int position) {
        ShopperModel shopper = shopperList.get(position);

        holder.tvShopperName.setText(shopper.getName());
        holder.tvRating.setText("⭐ " + shopper.getRating());
        holder.tvOrders.setText(" • " + shopper.getCompletedOrders() + " đơn đã hoàn thành");
        holder.tvDistance.setText("Cách bạn " + shopper.getDistance() + " km");

        Glide.with(context)
                .load(shopper.getAvatarUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.ivAvatar);

        holder.btnSelect.setOnClickListener(v -> {
            Toast.makeText(context, "Đã nhờ shopper " + shopper.getName() + " mua giùm!", Toast.LENGTH_SHORT).show();
            // Start Chat
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("SHOPPER_ID", shopper.getShopperId());
            intent.putExtra("SHOPPER_NAME", shopper.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return shopperList.size();
    }

    public static class ShopperViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvShopperName, tvRating, tvOrders, tvDistance;
        Button btnSelect;

        public ShopperViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvShopperName = itemView.findViewById(R.id.tvShopperName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvOrders = itemView.findViewById(R.id.tvOrders);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnSelect = itemView.findViewById(R.id.btnSelect);
        }
    }
}
