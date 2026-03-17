package com.example.gomarket.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.ShoppingRequestItem;

import java.util.List;

public class ShopperChecklistAdapter extends RecyclerView.Adapter<ShopperChecklistAdapter.ChecklistViewHolder> {

    private final List<ShoppingRequestItem> items;
    private final OnItemUpdateListener listener;

    public interface OnItemUpdateListener {
        void onItemChecked(ShoppingRequestItem item, boolean checked);
        void onPriceChanged(ShoppingRequestItem item, Double price);
    }

    public ShopperChecklistAdapter(List<ShoppingRequestItem> items, OnItemUpdateListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checklist, parent, false);
        return new ChecklistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
        ShoppingRequestItem item = items.get(position);

        holder.tvItemName.setText(item.getItemText());
        holder.tvQuantityNote.setText(item.getQuantityNote() != null ? item.getQuantityNote() : "");

        // Remove listeners before setting values to avoid loops
        holder.cbPurchased.setOnCheckedChangeListener(null);
        holder.cbPurchased.setChecked(item.isPurchased());

        if (item.getActualPrice() != null && item.getActualPrice() > 0) {
            holder.etPrice.setText(String.format("%,.0f", item.getActualPrice()));
        } else {
            holder.etPrice.setText("");
        }

        // Strikethrough if purchased
        if (item.isPurchased()) {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.cbPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setIsPurchased(isChecked);
            if (isChecked) {
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
            listener.onItemChecked(item, isChecked);
        });

        holder.etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String text = s.toString().replace(",", "").replace(".", "");
                    Double price = text.isEmpty() ? null : Double.parseDouble(text);
                    item.setActualPrice(price);
                    listener.onPriceChanged(item, price);
                } catch (NumberFormatException e) { /* ignore */ }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChecklistViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPurchased;
        TextView tvItemName, tvQuantityNote;
        EditText etPrice;

        ChecklistViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPurchased = itemView.findViewById(R.id.cbPurchased);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvQuantityNote = itemView.findViewById(R.id.tvQuantityNote);
            etPrice = itemView.findViewById(R.id.etPrice);
        }
    }
}
