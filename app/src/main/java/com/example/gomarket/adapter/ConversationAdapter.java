package com.example.gomarket.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;

import java.util.List;
import java.util.Map;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConvViewHolder> {

    private final List<Map<String, Object>> conversations;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Map<String, Object> conversation);
    }

    public ConversationAdapter(List<Map<String, Object>> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvViewHolder holder, int position) {
        Map<String, Object> conv = conversations.get(position);

        String name = (String) conv.get("otherUserName");
        holder.tvName.setText(name != null ? name : "Người dùng");

        String lastMsg = (String) conv.get("lastMessage");
        holder.tvLastMessage.setText(lastMsg != null ? lastMsg : "");

        // Time
        String timeStr = (String) conv.get("lastMessageTime");
        if (timeStr != null && timeStr.length() >= 16) {
            holder.tvTime.setText(timeStr.substring(11, 16));
        } else {
            holder.tvTime.setText("");
        }

        // Order ID
        Object reqIdObj = conv.get("requestId");
        if (reqIdObj != null) {
            long reqId = reqIdObj instanceof Number ? ((Number) reqIdObj).longValue() : 0;
            holder.tvOrderId.setText("Đơn #" + String.format("%03d", reqId));
        }

        // Status-based avatar
        String status = (String) conv.get("status");
        if ("COMPLETED".equals(status)) {
            holder.tvAvatar.setText("✅");
        } else if ("CANCELLED".equals(status)) {
            holder.tvAvatar.setText("❌");
        } else {
            holder.tvAvatar.setText("🛵");
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conv));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConvViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLastMessage, tvTime, tvOrderId;

        ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvConvAvatar);
            tvName = itemView.findViewById(R.id.tvConvName);
            tvLastMessage = itemView.findViewById(R.id.tvConvLastMessage);
            tvTime = itemView.findViewById(R.id.tvConvTime);
            tvOrderId = itemView.findViewById(R.id.tvConvOrderId);
        }
    }
}
