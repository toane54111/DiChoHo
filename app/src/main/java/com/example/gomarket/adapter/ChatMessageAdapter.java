package com.example.gomarket.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gomarket.R;
import com.example.gomarket.model.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> messages;
    private final long currentUserId;

    public ChatMessageAdapter(List<ChatMessage> messages, long currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean isMine = msg.getSenderId() == currentUserId;

        holder.tvMessage.setText(msg.getMessage());

        // Format time
        String time = "";
        if (msg.getCreatedAt() != null && msg.getCreatedAt().length() >= 16) {
            time = msg.getCreatedAt().substring(11, 16);
        }
        holder.tvTime.setText(time);

        if (isMine) {
            holder.container.setGravity(Gravity.END);
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_bubble_mine);
            holder.tvMessage.setTextColor(0xFFFFFFFF);
            holder.tvTime.setTextColor(0xCCFFFFFF);
            holder.tvSenderName.setVisibility(View.GONE);
        } else {
            holder.container.setGravity(Gravity.START);
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_bubble_other);
            holder.tvMessage.setTextColor(0xFF212121);
            holder.tvTime.setTextColor(0xFF9E9E9E);

            // Show sender name if first message or different sender than previous
            if (position == 0 || messages.get(position - 1).getSenderId() != msg.getSenderId()) {
                holder.tvSenderName.setVisibility(View.VISIBLE);
                holder.tvSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "");
            } else {
                holder.tvSenderName.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container, bubble;
        TextView tvMessage, tvTime, tvSenderName;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.chatMsgContainer);
            bubble = itemView.findViewById(R.id.chatBubble);
            tvMessage = itemView.findViewById(R.id.tvChatMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            tvSenderName = itemView.findViewById(R.id.tvChatSenderName);
        }
    }
}
