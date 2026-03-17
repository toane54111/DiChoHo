package com.example.gomarket.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.ShoppingRequest;
import com.example.gomarket.model.ShoppingRequestItem;

import java.util.List;

public class ShoppingRequestAdapter extends RecyclerView.Adapter<ShoppingRequestAdapter.RequestViewHolder> {

    private List<ShoppingRequest> requests;
    private final OnRequestClickListener listener;
    private double shopperLat = 0, shopperLng = 0;

    public interface OnRequestClickListener {
        void onRequestClick(ShoppingRequest request);
    }

    public ShoppingRequestAdapter(List<ShoppingRequest> requests, OnRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    public void setShopperLocation(double lat, double lng) {
        this.shopperLat = lat;
        this.shopperLng = lng;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        ShoppingRequest req = requests.get(position);

        holder.tvRequestId.setText("Đơn #" + String.format("%03d", req.getId()));
        holder.tvStatus.setText(getStatusText(req.getStatus()));

        // Hiển thị giá: hoàn thành → thực tế + phí, chưa xong → ước tính + phí
        double shopperFeeVal = req.getShopperFee() != null ? req.getShopperFee() : 0;
        if ("COMPLETED".equals(req.getStatus()) && req.getTotalActualCost() != null) {
            double displayTotal = req.getTotalActualCost() + shopperFeeVal;
            holder.tvBudget.setText(String.format("%,.0fđ", displayTotal));
        } else {
            double budgetVal = req.getBudget() != null ? req.getBudget() : 0;
            double displayTotal = budgetVal + shopperFeeVal;
            holder.tvBudget.setText(displayTotal > 0 ? String.format("~%,.0fđ", displayTotal) : "");
        }

        // Items preview
        StringBuilder itemsText = new StringBuilder();
        if (req.getItems() != null) {
            for (int i = 0; i < Math.min(req.getItems().size(), 3); i++) {
                ShoppingRequestItem item = req.getItems().get(i);
                if (i > 0) itemsText.append(", ");
                itemsText.append(item.getItemText());
                if (item.getQuantityNote() != null) {
                    itemsText.append(" ").append(item.getQuantityNote());
                }
            }
            if (req.getItems().size() > 3) {
                itemsText.append("... (+").append(req.getItems().size() - 3).append(")");
            }
        }
        holder.tvItems.setText(itemsText.toString());

        // Shopper fee
        if (req.getShopperFee() != null && req.getShopperFee() > 0) {
            holder.tvShopperFee.setVisibility(View.VISIBLE);
            double fee = req.getShopperFee();
            String feeText = fee >= 1000000 ? String.format("%.1fM", fee / 1000000)
                    : String.format("%.0fK", fee / 1000);
            holder.tvShopperFee.setText("🤝 Phí: " + feeText);
        } else {
            holder.tvShopperFee.setVisibility(View.GONE);
        }

        // Distance
        if (shopperLat != 0 && req.getLatitude() != null && req.getLongitude() != null) {
            double dist = haversine(shopperLat, shopperLng, req.getLatitude(), req.getLongitude());
            holder.tvDistance.setVisibility(View.VISIBLE);
            if (dist < 1) {
                holder.tvDistance.setText(String.format("📍 %dm", (int)(dist * 1000)));
            } else {
                holder.tvDistance.setText(String.format("📍 %.1f km", dist));
            }
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // Address
        holder.tvAddress.setText(req.getDeliveryAddress() != null ?
                "📍 " + req.getDeliveryAddress() : "");

        // Time
        holder.tvTime.setText(formatTime(req.getCreatedAt()));

        holder.itemView.setOnClickListener(v -> listener.onRequestClick(req));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<ShoppingRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        if (status == null) return "Không rõ";
        switch (status) {
            case "OPEN": return "Đang chờ";
            case "ACCEPTED": return "Đã nhận";
            case "SHOPPING": return "Đang mua";
            case "DELIVERING": return "Đang giao";
            case "COMPLETED": return "Hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }

    private String formatBudget(Double budget) {
        if (budget == null) return "";
        return String.format("%,.0fđ", budget);
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            if (createdAt.length() >= 16) {
                return createdAt.substring(11, 16) + " " + createdAt.substring(0, 10);
            }
        } catch (Exception e) { /* ignore */ }
        return createdAt;
    }

    /** Haversine formula — distance in km */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequestId, tvStatus, tvItems, tvAddress, tvTime, tvBudget;
        TextView tvShopperFee, tvDistance;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestId = itemView.findViewById(R.id.tvRequestId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItems = itemView.findViewById(R.id.tvItems);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvShopperFee = itemView.findViewById(R.id.tvShopperFee);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}
