package com.example.gomarket.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.R;
import com.example.gomarket.model.MessageModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    private Context context;
    private List<MessageModel> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<MessageModel> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);
        boolean isImage = message.isImageMessage();

        if (isSent && isImage) return VIEW_TYPE_SENT_IMAGE;
        if (isSent) return VIEW_TYPE_SENT;
        if (isImage) return VIEW_TYPE_RECEIVED_IMAGE;
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent_image, parent, false);
            return new SentImageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received_image, parent, false);
            return new ReceivedImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
        String timeStr = sdf.format(new Date(message.getTimestamp()));

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.tvMessage.setText(message.getText());
            sentHolder.tvTime.setText(timeStr);
        } else if (holder.getItemViewType() == VIEW_TYPE_SENT_IMAGE) {
            SentImageViewHolder sentImageHolder = (SentImageViewHolder) holder;
            sentImageHolder.tvMessage.setText(message.getText());
            sentImageHolder.tvTime.setText(timeStr);
            if (message.getImageUrl() != null) {
                Glide.with(context)
                        .load(message.getImageUrl())
                        .into(sentImageHolder.ivImage);
            }
        } else if (holder.getItemViewType() == VIEW_TYPE_RECEIVED_IMAGE) {
            ReceivedImageViewHolder receivedImageHolder = (ReceivedImageViewHolder) holder;
            receivedImageHolder.tvMessage.setText(message.getText());
            receivedImageHolder.tvTime.setText(timeStr);
            if (message.getImageUrl() != null) {
                Glide.with(context)
                        .load(message.getImageUrl())
                        .into(receivedImageHolder.ivImage);
            }
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.tvMessage.setText(message.getText());
            receivedHolder.tvTime.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    public static class SentImageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        public SentImageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
        }
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    public static class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        public ReceivedImageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivImage = itemView.findViewById(R.id.ivImage);
        }
    }
}
