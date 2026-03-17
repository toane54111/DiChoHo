package com.gomarket.repository;

import com.gomarket.model.ShopperReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShopperReviewRepository extends JpaRepository<ShopperReview, Long> {

    List<ShopperReview> findByShopperIdOrderByCreatedAtDesc(Long shopperId);

    Optional<ShopperReview> findByRequestId(Long requestId);

    @Query("SELECT AVG(r.rating) FROM ShopperReview r WHERE r.shopperId = :shopperId")
    Double getAverageRating(Long shopperId);

    @Query("SELECT COUNT(r) FROM ShopperReview r WHERE r.shopperId = :shopperId")
    Long getReviewCount(Long shopperId);
}
