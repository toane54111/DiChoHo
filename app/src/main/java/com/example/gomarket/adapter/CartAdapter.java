package com.example.gomarket.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.OrderItem;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<OrderItem> cartItems;
    private OnCartItemListener listener;

    public interface OnCartItemListener {
        void onQuantityChanged(int position, int newQuantity);
        void onItemRemoved(int position);
    }

    public CartAdapter(List<OrderItem> cartItems, OnCartItemListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        OrderItem item = cartItems.get(position);

        holder.tvProductName.setText(item.getProductName());
        holder.tvProductPrice.setText(String.format("%,.0fđ", item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.btnMinus.setOnClickListener(v -> {
            int qty = item.getQuantity();
            if (qty > 1) {
                item.setQuantity(qty - 1);
                holder.tvQuantity.setText(String.valueOf(qty - 1));
                if (listener != null) listener.onQuantityChanged(position, qty - 1);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            int qty = item.getQuantity();
            item.setQuantity(qty + 1);
            holder.tvQuantity.setText(String.valueOf(qty + 1));
            if (listener != null) listener.onQuantityChanged(position, qty + 1);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName, tvProductPrice, tvQuantity, btnMinus, btnPlus, btnDelete;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
