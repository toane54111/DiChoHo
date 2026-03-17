package com.example.gomarket.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.R;
import com.example.gomarket.model.PostComment;
import com.example.gomarket.network.ApiClient;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private List<PostComment> comments;

    public CommentAdapter(Context context, List<PostComment> comments) {
        this.context = context;
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        PostComment comment = comments.get(position);

        holder.tvAuthorName.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Ẩn danh");
        holder.tvContent.setText(comment.getContent());
        holder.tvTime.setText(formatTime(comment.getCreatedAt()));

        if (comment.getAuthorAvatar() != null && !comment.getAuthorAvatar().isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.getFullImageUrl(comment.getAuthorAvatar()))
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateComments(List<PostComment> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }

    public void addComment(PostComment comment) {
        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            if (createdAt.length() >= 10) return createdAt.substring(0, 10);
        } catch (Exception e) { /* ignore */ }
        return createdAt;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvAuthorName, tvContent, tvTime;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
