package com.gomarket.controller;

import com.gomarket.model.CookbookComment;
import com.gomarket.model.CookbookRecipe;
import com.gomarket.service.CookbookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cookbook")
@CrossOrigin(origins = "*")
public class CookbookController {

    private final CookbookService cookbookService;

    public CookbookController(CookbookService cookbookService) {
        this.cookbookService = cookbookService;
        // Seed system recipes on startup
        cookbookService.seedSystemRecipes();
    }

    /** POST /api/cookbook — Tạo công thức mới */
    @PostMapping
    public ResponseEntity<?> createRecipe(@RequestBody Map<String, Object> body) {
        try {
            CookbookRecipe recipe = cookbookService.createRecipe(body);
            return ResponseEntity.ok(recipe);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/cookbook/suggestions — Công thức gợi ý (hệ thống) */
    @GetMapping("/suggestions")
    public ResponseEntity<List<CookbookRecipe>> getSuggestions(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(cookbookService.getSuggestions(page));
    }

    /** GET /api/cookbook/community — Công thức cộng đồng (sắp theo lượt tim) */
    @GetMapping("/community")
    public ResponseEntity<List<CookbookRecipe>> getCommunityRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(cookbookService.getCommunityRecipes(page, userId));
    }

    /** GET /api/cookbook/personal/{userId} — Công thức cá nhân (tự tạo + yêu thích) */
    @GetMapping("/personal/{userId}")
    public ResponseEntity<List<CookbookRecipe>> getPersonalRecipes(@PathVariable Long userId) {
        return ResponseEntity.ok(cookbookService.getPersonalRecipes(userId));
    }

    /** GET /api/cookbook/{id} — Chi tiết công thức */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipe(@PathVariable Long id,
                                        @RequestParam(required = false) Long userId) {
        try {
            return ResponseEntity.ok(cookbookService.getRecipe(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/cookbook/{id}/like — Toggle like */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id,
                                         @RequestParam Long userId) {
        return ResponseEntity.ok(cookbookService.toggleLike(id, userId));
    }

    /** POST /api/cookbook/{id}/comments — Thêm bình luận */
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body) {
        try {
            Long userId = ((Number) body.get("userId")).longValue();
            String content = (String) body.get("content");
            CookbookComment comment = cookbookService.addComment(id, userId, content);
            return ResponseEntity.ok(comment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/cookbook/{id}/comments — Danh sách bình luận */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CookbookComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(cookbookService.getComments(id));
    }

    /** DELETE /api/cookbook/{id} — Xóa công thức */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id,
                                           @RequestParam Long userId) {
        try {
            cookbookService.deleteRecipe(id, userId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa công thức"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
