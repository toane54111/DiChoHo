package com.gomarket.service;

import com.gomarket.model.*;
import com.gomarket.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository likeRepository,
                       PostCommentRepository commentRepository,
                       UserRepository userRepository,
                       EmbeddingService embeddingService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public Post createPost(Map<String, Object> body) {
        Post post = new Post();
        post.setUserId(toLong(body.get("userId")));
        post.setTitle((String) body.get("title"));
        post.setContent((String) body.get("content"));
        post.setCategory((String) body.get("category"));
        post.setLatitude(toDouble(body.get("latitude")));
        post.setLongitude(toDouble(body.get("longitude")));
        post.setLocationName((String) body.get("locationName"));
        post.setRegion((String) body.get("region"));

        // RAG: Embed nội dung bài đăng
        try {
            String textToEmbed = post.getTitle() + " " + post.getContent();
            float[] embedding = embeddingService.embed(textToEmbed);
            post.setEmbedding(embedding);
        } catch (Exception e) {
            System.err.println("Không thể embed bài đăng: " + e.getMessage());
        }

        Post saved = postRepository.save(post);

        // Save images
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) body.get("imageUrls");
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage img = new PostImage(imageUrls.get(i), i);
                img.setPost(saved);
                saved.getImages().add(img);
            }
            saved = postRepository.save(saved);
        }

        enrichPost(saved, null);
        return saved;
    }

    public List<Post> getFeed(Double lat, Double lng, int page, String category, String region) {
        List<Post> posts;

        if (lat != null && lng != null) {
            // Location-based feed
            posts = postRepository.findNearbyPosts(lat, lng, 20, page * 20);
        } else if (region != null && !region.isEmpty()) {
            // Filter theo vùng miền
            Page<Post> p = postRepository.findByIsActiveTrueAndRegionOrderByCreatedAtDesc(
                    region, PageRequest.of(page, 20));
            posts = p.getContent();
        } else if (category != null && !category.isEmpty()) {
            Page<Post> p = postRepository.findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(
                    category, PageRequest.of(page, 20));
            posts = p.getContent();
        } else {
            Page<Post> p = postRepository.findByIsActiveTrueOrderByCreatedAtDesc(
                    PageRequest.of(page, 20));
            posts = p.getContent();
        }

        posts.forEach(post -> enrichPost(post, null));
        return posts;
    }

    /**
     * RAG Search: Tìm bài đăng tương đồng ngữ nghĩa
     * "mực, tôm" sẽ match "hải sản tươi dưới biển lên"
     */
    public List<Post> searchPosts(String query) {
        try {
            float[] queryVector = embeddingService.embed(query);
            String vectorStr = Arrays.toString(queryVector);
            List<Post> posts = postRepository.searchByVector(vectorStr, 20);
            posts.forEach(post -> enrichPost(post, null));
            return posts;
        } catch (Exception e) {
            System.err.println("Vector search thất bại: " + e.getMessage());
            return List.of();
        }
    }

    public Post getPost(Long id, Long viewerUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng"));
        enrichPost(post, viewerUserId);
        return post;
    }

    public List<Post> getUserPosts(Long userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        posts.forEach(post -> enrichPost(post, null));
        return posts;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        var existing = likeRepository.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(new PostLike(postId, userId));
            liked = true;
        }
        int count = likeRepository.countByPostId(postId);
        return Map.of("liked", liked, "likeCount", count);
    }

    @Transactional
    public PostComment addComment(Long postId, Long userId, String content) {
        PostComment comment = new PostComment(postId, userId, content);
        comment = commentRepository.save(comment);
        enrichComment(comment);
        return comment;
    }

    public List<PostComment> getComments(Long postId) {
        List<PostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        comments.forEach(this::enrichComment);
        return comments;
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng"));
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài đăng này");
        }
        post.setIsActive(false);
        postRepository.save(post);
    }

    private void enrichPost(Post post, Long viewerUserId) {
        userRepository.findById(post.getUserId()).ifPresent(user -> {
            post.setAuthorName(user.getFullName());
            post.setAuthorAvatar(user.getAvatarUrl());
            post.setAuthorPhone(user.getPhone());
        });
        post.setLikeCount(likeRepository.countByPostId(post.getId()));
        post.setCommentCount(commentRepository.countByPostId(post.getId()));
        if (viewerUserId != null) {
            post.setIsLikedByUser(likeRepository.existsByPostIdAndUserId(post.getId(), viewerUserId));
        }
    }

    private void enrichComment(PostComment comment) {
        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            comment.setAuthorName(user.getFullName());
            comment.setAuthorAvatar(user.getAvatarUrl());
        });
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }
}
