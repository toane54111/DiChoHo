package com.gomarket.repository;

import com.gomarket.model.CookbookLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CookbookLikeRepository extends JpaRepository<CookbookLike, Long> {
    Optional<CookbookLike> findByRecipeIdAndUserId(Long recipeId, Long userId);
    int countByRecipeId(Long recipeId);
    boolean existsByRecipeIdAndUserId(Long recipeId, Long userId);
}
