package com.example.gomarket.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.R;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.network.ApiClient;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class PostFeedAdapter extends RecyclerView.Adapter<PostFeedAdapter.PostViewHolder> {

    private final Context context;
    private List<CommunityPost> posts;
    private final OnPostActionListener listener;

    public interface OnPostActionListener {
        void onPostClick(CommunityPost post);
        void onLikeClick(CommunityPost post, int position);
        void onCommentClick(CommunityPost post);
        void onContactClick(CommunityPost post);
    }

    public PostFeedAdapter(Context context, List<CommunityPost> posts, OnPostActionListener listener) {
        this.context = context;
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_feed, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityPost post = posts.get(position);

        holder.tvAuthorName.setText(post.getAuthorName() != null ? post.getAuthorName() : "Ẩn danh");
        holder.tvLocation.setText(post.getLocationName() != null ? post.getLocationName() : "");
        holder.tvTime.setText(formatTime(post.getCreatedAt()));
        holder.tvCategory.setText(post.getCategoryDisplay());
        holder.tvTitle.setText(post.getTitle());
        holder.tvContent.setText(post.getContent());

        // Like state
        boolean liked = post.getIsLikedByUser() != null && post.getIsLikedByUser();
        holder.tvLikeIcon.setText(liked ? "♥" : "♡");
        holder.tvLikeIcon.setTextColor(liked ?
                context.getColor(R.color.status_busy) : context.getColor(R.color.text_secondary));
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // Avatar
        if (post.getAuthorAvatar() != null && !post.getAuthorAvatar().isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.getFullImageUrl(post.getAuthorAvatar()))
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivAuthorAvatar);
        }

        // Post image
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            holder.cardImage.setVisibility(View.VISIBLE);
            String imageUrl = ApiClient.getFullImageUrl(post.getImages().get(0).getImageUrl());
            Glide.with(context).load(imageUrl).centerCrop().into(holder.ivPostImage);
        } else {
            holder.cardImage.setVisibility(View.GONE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> listener.onPostClick(post));
        holder.btnLike.setOnClickListener(v -> listener.onLikeClick(post, position));
        holder.btnComment.setOnClickListener(v -> listener.onCommentClick(post));
        holder.btnContact.setOnClickListener(v -> listener.onContactClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updatePosts(List<CommunityPost> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    public void addPosts(List<CommunityPost> morePosts) {
        int start = posts.size();
        posts.addAll(morePosts);
        notifyItemRangeInserted(start, morePosts.size());
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            // Simple relative time - just show the date part
            if (createdAt.length() >= 10) {
                return createdAt.substring(0, 10);
            }
        } catch (Exception e) { /* ignore */ }
        return createdAt;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAuthorAvatar;
        TextView tvAuthorName, tvLocation, tvTime, tvCategory, tvTitle, tvContent;
        MaterialCardView cardImage;
        ImageView ivPostImage;
        TextView tvLikeIcon, tvLikeCount, tvCommentCount;
        LinearLayout btnLike, btnComment, btnContact;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            cardImage = itemView.findViewById(R.id.cardImage);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvLikeIcon = itemView.findViewById(R.id.tvLikeIcon);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnContact = itemView.findViewById(R.id.btnContact);
        }
    }
}
