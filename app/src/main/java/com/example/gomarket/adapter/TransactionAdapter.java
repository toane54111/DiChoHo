package com.example.gomarket.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.WalletTransaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<WalletTransaction> transactions;

    public TransactionAdapter(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction tx = transactions.get(position);

        // Icon và màu theo loại
        switch (tx.getType()) {
            case "TOP_UP":
                holder.tvIcon.setText("+");
                holder.layoutIcon.setBackgroundResource(R.drawable.bg_icon_green);
                break;
            case "PAYMENT":
                holder.tvIcon.setText("-");
                holder.layoutIcon.setBackgroundResource(R.drawable.bg_icon_orange);
                break;
            case "REFUND":
                holder.tvIcon.setText("R");
                holder.layoutIcon.setBackgroundResource(R.drawable.bg_icon_blue);
                break;
            default:
                holder.tvIcon.setText("?");
                holder.layoutIcon.setBackgroundResource(R.drawable.bg_icon_gray);
        }

        // Mô tả
        holder.tvDescription.setText(tx.getDescription() != null ? tx.getDescription() : tx.getType());

        // Thời gian - parse ISO datetime
        String time = tx.getCreatedAt();
        if (time != null && time.length() >= 16) {
            // "2026-03-15T10:30:00" → "15/03 10:30"
            String day = time.substring(8, 10);
            String month = time.substring(5, 7);
            String hour = time.substring(11, 16);
            holder.tvTime.setText(day + "/" + month + " " + hour);
        }

        // Số tiền
        long amount = tx.getAmount();
        if (amount >= 0) {
            holder.tvAmount.setText("+" + String.format("%,d", amount) + "d");
            holder.tvAmount.setTextColor(0xFF4CAF50); // green
        } else {
            holder.tvAmount.setText(String.format("%,d", amount) + "d");
            holder.tvAmount.setTextColor(0xFFF44336); // red
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIcon;
        TextView tvIcon, tvDescription, tvTime, tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIcon = itemView.findViewById(R.id.layoutIcon);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
