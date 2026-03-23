package com.gomarket.repository;

import com.gomarket.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByIsActiveTrueAndCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    Page<Post> findByIsActiveTrueAndRegionOrderByCreatedAtDesc(String region, Pageable pageable);

    Page<Post> findByIsActiveTrueAndProvinceOrderByCreatedAtDesc(String province, Pageable pageable);

    Page<Post> findByIsActiveTrueAndRegionAndProvinceOrderByCreatedAtDesc(String region, String province, Pageable pageable);

    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Text-based search fallback khi vector search không khả dụng
     */
    @Query("SELECT p FROM Post p WHERE p.isActive = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.locationName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.province) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Post> searchByText(@Param("q") String query);

    /**
     * RAG: Vector search trên posts — tìm bài đăng tương đồng ngữ nghĩa
     */
    @Query(value = """
        SELECT p.*, (1 - (p.embedding <=> cast(:vector as vector))) as similarity
        FROM posts p
        WHERE p.is_active = true
        AND p.embedding IS NOT NULL
        ORDER BY p.embedding <=> cast(:vector as vector) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Post> searchByVector(@Param("vector") String vector, @Param("limit") int limit);

    /**
     * Feed sắp xếp theo khoảng cách (gần nhất trước)
     */
    @Query(value = """
        SELECT * FROM posts p
        WHERE p.is_active = true
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude))
            * cos(radians(p.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(p.latitude)))) ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Post> findNearbyPosts(
            @Param("lat") double lat, @Param("lng") double lng,
            @Param("limit") int limit, @Param("offset") int offset);
}
