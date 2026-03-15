package com.example.gomarket.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.ChatActivity;
import com.example.gomarket.R;
import com.example.gomarket.model.ShopperModel;

import java.util.List;

public class ShopperAdapter extends RecyclerView.Adapter<ShopperAdapter.ShopperViewHolder> {

    private static final String[][] MOCK_LOCATIONS = {
            {"Đang ở chợ Bến Thành", "1.2"},
            {"Đang ở Vinmart Quận 1", "0.8"},
            {"Đang trên đường", "2.1"},
            {"Đang ở siêu thị CoopMart", "1.5"}
    };
    private static final String[][] MOCK_SKILLS = {
            {"Chọn thịt tươi giỏi", "Quen chợ Bến Thành"},
            {"Mua siêu thị nhanh", "Đóng gói cẩn thận"},
            {"Giao hàng đúng giờ", "Thân thiện"},
            {"Giá tốt", "Nhiều kinh nghiệm"}
    };
    private static final String[] MOCK_SHIP = {
            "Phí ship ~15.000₫ • 20-30 phút",
            "Phí ship ~12.000₫ • 15-25 phút",
            "Phí ship ~20.000₫ • 35-45 phút",
            "Phí ship ~18.000₫ • 25-35 phút"
    };

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
        holder.tvRating.setText(String.format("%.1f", shopper.getRating()));
        holder.tvOrders.setText("(" + shopper.getCompletedOrders() + " đơn)");

        int i = Math.min(position, MOCK_LOCATIONS.length - 1);
        holder.tvLocation.setText(MOCK_LOCATIONS[i][0] + " • cách " + String.format("%.1f", shopper.getDistance()).replace(",", ".") + "km");
        holder.tvShipFee.setText(MOCK_SHIP[i]);
        holder.tvShipFee.setVisibility(View.VISIBLE);

        holder.badgePhuHop.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        if (position == 0) {
            holder.cardShopper.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_light));
        } else {
            holder.cardShopper.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        }

        holder.skill1.setVisibility(View.VISIBLE);
        holder.skill1.setText(MOCK_SKILLS[i][0]);
        holder.skill2.setVisibility(View.VISIBLE);
        holder.skill2.setText(MOCK_SKILLS[i][1]);
        holder.tvFastDelivery.setVisibility(position < 2 ? View.VISIBLE : View.GONE);
        if (position < 2) holder.tvFastDelivery.setText("Giao nhanh");

        if (shopper.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(ContextCompat.getColor(context, R.color.status_online));
            drawable.setStroke(2, ContextCompat.getColor(context, R.color.white));
            holder.onlineIndicator.setBackground(drawable);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(shopper.getAvatarUrl())
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .circleCrop()
                .into(holder.ivAvatar);

        holder.btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("SHOPPER_ID", shopper.getShopperId());
            intent.putExtra("SHOPPER_NAME", shopper.getName());
            intent.putExtra("SHOPPER_AVATAR", shopper.getAvatarUrl());
            if (missingIngredients != null) intent.putStringArrayListExtra("MISSING_INGREDIENTS", new java.util.ArrayList<>(missingIngredients));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return shopperList.size();
    }

    public static class ShopperViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardShopper;
        ImageView ivAvatar;
        View onlineIndicator;
        TextView tvShopperName, tvRating, tvOrders, tvLocation, skill1, skill2, tvFastDelivery, tvShipFee, badgePhuHop, btnSelect;

        public ShopperViewHolder(@NonNull View itemView) {
            super(itemView);
            cardShopper = itemView.findViewById(R.id.cardShopper);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            tvShopperName = itemView.findViewById(R.id.tvShopperName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvOrders = itemView.findViewById(R.id.tvOrders);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            skill1 = itemView.findViewById(R.id.skill1);
            skill2 = itemView.findViewById(R.id.skill2);
            tvFastDelivery = itemView.findViewById(R.id.tvFastDelivery);
            tvShipFee = itemView.findViewById(R.id.tvShipFee);
            badgePhuHop = itemView.findViewById(R.id.badgePhuHop);
            btnSelect = itemView.findViewById(R.id.btnSelect);
        }
    }
}
