package com.example.gomarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gomarket.adapter.CommentAdapter;
import com.example.gomarket.model.CommunityPost;
import com.example.gomarket.model.PostComment;
import com.example.gomarket.network.ApiClient;
import com.example.gomarket.network.ApiService;
import com.example.gomarket.util.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private ShapeableImageView ivAuthorAvatar;
    private TextView tvAuthorName, tvLocation, tvTime, tvCategory, tvTitle, tvContent;
    private TextView tvLikeIcon, tvLikeCount, tvPhone, tvCommentsHeader;
    private MaterialCardView cardImage;
    private ImageView ivPostImage;
    private RecyclerView rvComments;
    private EditText etComment;

    private CommentAdapter commentAdapter;
    private List<PostComment> comments = new ArrayList<>();

    private ApiService apiService;
    private SessionManager session;
    private long postId;
    private CommunityPost currentPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        apiService = ApiClient.getApiService(this);
        session = new SessionManager(this);
        postId = getIntent().getLongExtra("POST_ID", -1);

        initViews();
        loadPost();
        loadComments();
    }

    private void initViews() {
        ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        tvLocation = findViewById(R.id.tvLocation);
        tvTime = findViewById(R.id.tvTime);
        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        tvLikeIcon = findViewById(R.id.tvLikeIcon);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvPhone = findViewById(R.id.tvPhone);
        tvCommentsHeader = findViewById(R.id.tvCommentsHeader);
        cardImage = findViewById(R.id.cardImage);
        ivPostImage = findViewById(R.id.ivPostImage);
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        commentAdapter = new CommentAdapter(this, comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        findViewById(R.id.btnLike).setOnClickListener(v -> toggleLike());
        findViewById(R.id.btnContact).setOnClickListener(v -> callAuthor());
        findViewById(R.id.btnSendComment).setOnClickListener(v -> sendComment());

        // Focus comment if requested
        if (getIntent().getBooleanExtra("FOCUS_COMMENT", false)) {
            etComment.requestFocus();
        }
    }

    private void loadPost() {
        apiService.getPost(postId, (long) session.getUserId()).enqueue(new Callback<CommunityPost>() {
            @Override
            public void onResponse(Call<CommunityPost> call, Response<CommunityPost> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentPost = response.body();
                    displayPost();
                }
            }
            @Override
            public void onFailure(Call<CommunityPost> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi tải bài đăng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPost() {
        tvAuthorName.setText(currentPost.getAuthorName() != null ? currentPost.getAuthorName() : "Ẩn danh");
        tvLocation.setText(currentPost.getLocationName() != null ? currentPost.getLocationName() : "");
        tvTime.setText(currentPost.getCreatedAt() != null ? currentPost.getCreatedAt().substring(0, Math.min(10, currentPost.getCreatedAt().length())) : "");
        tvCategory.setText(currentPost.getCategoryDisplay());
        tvTitle.setText(currentPost.getTitle());
        tvContent.setText(currentPost.getContent());

        // Phone
        tvPhone.setText(currentPost.getAuthorPhone() != null ? currentPost.getAuthorPhone() : "Liên hệ");

        // Like state
        boolean liked = currentPost.getIsLikedByUser() != null && currentPost.getIsLikedByUser();
        tvLikeIcon.setText(liked ? "♥" : "♡");
        tvLikeIcon.setTextColor(liked ? getColor(R.color.status_busy) : getColor(R.color.text_secondary));
        tvLikeCount.setText(String.valueOf(currentPost.getLikeCount()));

        // Avatar
        if (currentPost.getAuthorAvatar() != null && !currentPost.getAuthorAvatar().isEmpty()) {
            Glide.with(this)
                    .load(ApiClient.getFullImageUrl(currentPost.getAuthorAvatar()))
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(ivAuthorAvatar);
        }

        // Image
        if (currentPost.getImages() != null && !currentPost.getImages().isEmpty()) {
            cardImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(ApiClient.getFullImageUrl(currentPost.getImages().get(0).getImageUrl()))
                    .centerCrop()
                    .into(ivPostImage);
        }
    }

    private void loadComments() {
        apiService.getComments(postId).enqueue(new Callback<List<PostComment>>() {
            @Override
            public void onResponse(Call<List<PostComment>> call, Response<List<PostComment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comments.clear();
                    comments.addAll(response.body());
                    commentAdapter.updateComments(comments);
                    tvCommentsHeader.setText("💬 Bình luận (" + comments.size() + ")");
                }
            }
            @Override public void onFailure(Call<List<PostComment>> call, Throwable t) {}
        });
    }

    private void toggleLike() {
        apiService.toggleLike(postId, session.getUserId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean liked = Boolean.TRUE.equals(response.body().get("liked"));
                    int count = ((Number) response.body().get("likeCount")).intValue();
                    tvLikeIcon.setText(liked ? "♥" : "♡");
                    tvLikeIcon.setTextColor(liked ? getColor(R.color.status_busy) : getColor(R.color.text_secondary));
                    tvLikeCount.setText(String.valueOf(count));
                    if (currentPost != null) {
                        currentPost.setIsLikedByUser(liked);
                        currentPost.setLikeCount(count);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void callAuthor() {
        if (currentPost != null && currentPost.getAuthorPhone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + currentPost.getAuthorPhone()));
            startActivity(intent);
        }
    }

    private void sendComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> body = new HashMap<>();
        body.put("userId", (long) session.getUserId());
        body.put("content", text);

        apiService.addComment(postId, body).enqueue(new Callback<PostComment>() {
            @Override
            public void onResponse(Call<PostComment> call, Response<PostComment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.addComment(response.body());
                    etComment.setText("");
                    tvCommentsHeader.setText("💬 Bình luận (" + comments.size() + ")");
                    rvComments.scrollToPosition(comments.size() - 1);
                }
            }
            @Override
            public void onFailure(Call<PostComment> call, Throwable t) {
                Toast.makeText(PostDetailActivity.this, "Lỗi gửi bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
