package com.example.gomarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private ArrayList<ChatMessage> messages;

    public ChatMessageAdapter(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isMe() ? VIEW_TYPE_ME : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ME) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_me, parent, false);
            return new MeViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_other, parent, false);
            return new OtherViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof MeViewHolder) {
            ((MeViewHolder) holder).textViewMessageMe.setText(message.getMessage());
        } else if (holder instanceof OtherViewHolder) {
            ((OtherViewHolder) holder).textViewMessageOther.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder cho tin nhắn của mình
    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessageMe;

        MeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessageMe = itemView.findViewById(R.id.textViewMessageMe);
        }
    }

    // ViewHolder cho tin nhắn người khác
    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessageOther;

        OtherViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessageOther = itemView.findViewById(R.id.textViewMessageOther);
        }
    }
}
