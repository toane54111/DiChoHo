package com.gomarket.repository;

import com.gomarket.model.ShoppingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShoppingRequestRepository extends JpaRepository<ShoppingRequest, Long> {

    List<ShoppingRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ShoppingRequest> findByShopperIdOrderByCreatedAtDesc(Long shopperId);

    List<ShoppingRequest> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Tìm đơn OPEN gần vị trí shopper (Haversine formula, bán kính km)
     */
    @Query(value = """
        SELECT * FROM shopping_requests sr
        WHERE sr.status = 'OPEN'
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(sr.latitude))
            * cos(radians(sr.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(sr.latitude)))) <= :radiusKm
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(sr.latitude))
            * cos(radians(sr.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(sr.latitude)))) ASC
        """, nativeQuery = true)
    List<ShoppingRequest> findNearbyOpenRequests(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);
}
