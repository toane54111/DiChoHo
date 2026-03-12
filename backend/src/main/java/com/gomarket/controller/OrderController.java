package com.gomarket.controller;

import com.gomarket.dto.OrderRequest;
import com.gomarket.model.Order;
import com.gomarket.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/orders
     * Tạo đơn hàng mới
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/orders/{id}
     * Lấy chi tiết đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            Order order = orderService.getOrder(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/orders/user/{userId}
     * Lấy tất cả đơn hàng của user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * PUT /api/orders/{id}/status
     * Cập nhật trạng thái đơn hàng
     * Body: {"status": "SHOPPING" | "DELIVERING" | "COMPLETED" | "CANCELLED"}
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Trạng thái không được để trống"));
            }
            Order order = orderService.updateStatus(id, status);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
