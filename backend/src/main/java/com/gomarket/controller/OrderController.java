package com.gomarket.controller;

import com.gomarket.dto.LocationResponse;
import com.gomarket.dto.OrderRequest;
import com.gomarket.model.Order;
import com.gomarket.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** POST /api/orders — Tạo đơn hàng mới */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.createOrder(request);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/orders/{id} — Lấy chi tiết đơn hàng */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            Order order = orderService.getOrder(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/orders/user/{userId} — Lấy tất cả đơn hàng của user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    /** PUT /api/orders/{id}/status — Cập nhật trạng thái đơn hàng */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không được để trống"));
            return ResponseEntity.ok(orderService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/orders/{id}/location — Shopper cập nhật vị trí GPS
     * Body: {"shopperLat": 10.77, "shopperLng": 106.70}
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long id,
                                            @RequestBody Map<String, Double> body) {
        try {
            Double lat = body.get("shopperLat");
            Double lng = body.get("shopperLng");
            if (lat == null || lng == null)
                return ResponseEntity.badRequest().body("Thiếu tọa độ");
            orderService.updateLocation(id, lat, lng);
            return ResponseEntity.ok(Map.of("message", "Cập nhật vị trí thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/orders/{id}/location — Buyer lấy vị trí GPS mới nhất của Shopper
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<?> getLocation(@PathVariable Long id) {
        try {
            Order order = orderService.getOrder(id);
            LocationResponse response = new LocationResponse(
                    order.getShopperLat(), order.getShopperLng(),
                    order.getShopperName(), order.getShopperPhone(),
                    order.getLocationUpdatedAt()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
