package com.gomarket.controller;

import com.gomarket.model.ShopperReview;
import com.gomarket.model.User;
import com.gomarket.repository.ShopperReviewRepository;
import com.gomarket.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ShopperReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewController(ShopperReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    /** POST /api/reviews — Tạo đánh giá shopper */
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> body) {
        try {
            Long requestId = ((Number) body.get("requestId")).longValue();
            Long buyerId = ((Number) body.get("buyerId")).longValue();
            Long shopperId = ((Number) body.get("shopperId")).longValue();
            Integer rating = ((Number) body.get("rating")).intValue();
            String comment = (String) body.getOrDefault("comment", "");

            // Check if already reviewed
            if (reviewRepository.findByRequestId(requestId).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã đánh giá đơn này rồi"));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating phải từ 1-5"));
            }

            ShopperReview review = new ShopperReview();
            review.setRequestId(requestId);
            review.setBuyerId(buyerId);
            review.setShopperId(shopperId);
            review.setRating(rating);
            review.setComment(comment);

            ShopperReview saved = reviewRepository.save(review);

            // Update shopper's average rating
            Double avg = reviewRepository.getAverageRating(shopperId);
            if (avg != null) {
                userRepository.findById(shopperId).ifPresent(shopper -> {
                    shopper.setRating(avg);
                    userRepository.save(shopper);
                });
            }

            // Enrich with buyer name
            userRepository.findById(buyerId).ifPresent(buyer ->
                    saved.setBuyerName(buyer.getFullName()));

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/reviews/shopper/{shopperId} — Danh sách đánh giá */
    @GetMapping("/shopper/{shopperId}")
    public ResponseEntity<List<ShopperReview>> getShopperReviews(@PathVariable Long shopperId) {
        List<ShopperReview> reviews = reviewRepository.findByShopperIdOrderByCreatedAtDesc(shopperId);
        reviews.forEach(r -> userRepository.findById(r.getBuyerId())
                .ifPresent(buyer -> r.setBuyerName(buyer.getFullName())));
        return ResponseEntity.ok(reviews);
    }

    /** GET /api/reviews/shopper/{shopperId}/summary — Rating trung bình */
    @GetMapping("/shopper/{shopperId}/summary")
    public ResponseEntity<Map<String, Object>> getShopperRatingSummary(@PathVariable Long shopperId) {
        Double avg = reviewRepository.getAverageRating(shopperId);
        Long count = reviewRepository.getReviewCount(shopperId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageRating", avg != null ? avg : 5.0);
        result.put("totalReviews", count != null ? count : 0);
        return ResponseEntity.ok(result);
    }

    /** GET /api/reviews/request/{requestId} — Check if reviewed */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<?> getReviewForRequest(@PathVariable Long requestId) {
        return reviewRepository.findByRequestId(requestId)
                .map(r -> {
                    userRepository.findById(r.getBuyerId())
                            .ifPresent(buyer -> r.setBuyerName(buyer.getFullName()));
                    return ResponseEntity.ok((Object) r);
                })
                .orElse(ResponseEntity.ok(Map.of("reviewed", false)));
    }
}
