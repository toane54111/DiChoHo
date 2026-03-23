package com.gomarket.controller;

import com.gomarket.model.Post;
import com.gomarket.model.PostComment;
import com.gomarket.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
        postService.seedCommunityPosts();
    }

    /** POST /api/posts — Tạo bài đăng mới */
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> body) {
        try {
            Post post = postService.createPost(body);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/posts/feed — Feed bài đăng (location-based hoặc mới nhất) */
    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String province) {
        return ResponseEntity.ok(postService.getFeed(lat, lng, page, category, region, province));
    }

    /** GET /api/posts/provinces — Danh sách tỉnh thành theo vùng miền */
    @GetMapping("/provinces")
    public ResponseEntity<java.util.Map<String, java.util.List<String>>> getProvinces() {
        return ResponseEntity.ok(PostService.getProvincesByRegion());
    }

    /** GET /api/posts/search?q= — RAG semantic search trên bài đăng */
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String q) {
        return ResponseEntity.ok(postService.searchPosts(q));
    }

    /** GET /api/posts/{id} — Chi tiết bài đăng */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id,
                                      @RequestParam(required = false) Long userId) {
        try {
            return ResponseEntity.ok(postService.getPost(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/posts/user/{userId} — Bài đăng của user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable Long userId) {
        return ResponseEntity.ok(postService.getUserPosts(userId));
    }

    /** POST /api/posts/{id}/like — Toggle like */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id,
                                         @RequestParam Long userId) {
        return ResponseEntity.ok(postService.toggleLike(id, userId));
    }

    /** POST /api/posts/{id}/comments — Thêm comment */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        try {
            Long userId = ((Number) body.get("userId")).longValue();
            String content = (String) body.get("content");
            PostComment comment = postService.addComment(id, userId, content);
            return ResponseEntity.ok(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/posts/{id}/comments — Danh sách comments */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<PostComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getComments(id));
    }

    /** DELETE /api/posts/{id} — Xóa bài đăng (soft delete) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id,
                                         @RequestParam Long userId) {
        try {
            postService.deletePost(id, userId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa bài đăng"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
