package com.gomarket.repository;

import com.gomarket.model.CookbookRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CookbookRecipeRepository extends JpaRepository<CookbookRecipe, Long> {

    // System recipes (gợi ý)
    Page<CookbookRecipe> findByIsSystemRecipeTrueOrderByCreatedAtDesc(Pageable pageable);

    // Community recipes (not system), sorted by like count
    @Query("SELECT r FROM CookbookRecipe r WHERE r.isSystemRecipe = false ORDER BY " +
           "(SELECT COUNT(l) FROM CookbookLike l WHERE l.recipeId = r.id) DESC, r.createdAt DESC")
    Page<CookbookRecipe> findCommunityRecipesByPopularity(Pageable pageable);

    // User's own recipes
    List<CookbookRecipe> findByUserIdOrderByCreatedAtDesc(Long userId);

    // User's liked recipes
    @Query("SELECT r FROM CookbookRecipe r WHERE r.id IN " +
           "(SELECT l.recipeId FROM CookbookLike l WHERE l.userId = :userId) " +
           "ORDER BY r.createdAt DESC")
    List<CookbookRecipe> findLikedByUser(Long userId);
}
