package com.gomarket.repository;

import com.gomarket.model.CookbookComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CookbookCommentRepository extends JpaRepository<CookbookComment, Long> {
    List<CookbookComment> findByRecipeIdOrderByCreatedAtAsc(Long recipeId);
    int countByRecipeId(Long recipeId);
}
