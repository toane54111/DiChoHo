package com.gomarket.repository;

import com.gomarket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    List<User> findByRoleAndIsOnlineTrue(String role);

    @Query(value = """
        SELECT * FROM users u
        WHERE u.role = 'SHOPPER' AND u.is_online = true
        AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(u.latitude))
            * cos(radians(u.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(u.latitude)))) <= :radiusKm
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(u.latitude))
            * cos(radians(u.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(u.latitude)))) ASC
        """, nativeQuery = true)
    List<User> findNearbyShoppers(@Param("lat") double lat,
                                   @Param("lng") double lng,
                                   @Param("radiusKm") double radiusKm);
}
