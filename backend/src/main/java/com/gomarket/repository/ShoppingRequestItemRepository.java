package com.gomarket.repository;

import com.gomarket.model.ShoppingRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingRequestItemRepository extends JpaRepository<ShoppingRequestItem, Long> {
}
