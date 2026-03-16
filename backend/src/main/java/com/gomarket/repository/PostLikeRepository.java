package com.gomarket.repository;

import com.gomarket.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    int countByPostId(Long postId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
