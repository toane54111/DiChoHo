package com.gomarket.controller;

import com.gomarket.model.ShoppingRequest;
import com.gomarket.model.ShoppingRequestItem;
import com.gomarket.service.ShoppingRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shopping-requests")
@CrossOrigin(origins = "*")
public class ShoppingRequestController {

    private final ShoppingRequestService service;

    public ShoppingRequestController(ShoppingRequestService service) {
        this.service = service;
    }

    /** POST /api/shopping-requests — Tạo đơn đi chợ hộ */
    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> body) {
        try {
            ShoppingRequest request = service.createRequest(body);
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/shopping-requests/{id} — Chi tiết đơn */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getRequest(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** GET /api/shopping-requests/user/{userId} — Đơn của buyer */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ShoppingRequest>> getUserRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserRequests(userId));
    }

    /** GET /api/shopping-requests/shopper/{shopperId} — Đơn của shopper */
    @GetMapping("/shopper/{shopperId}")
    public ResponseEntity<List<ShoppingRequest>> getShopperRequests(@PathVariable Long shopperId) {
        return ResponseEntity.ok(service.getShopperRequests(shopperId));
    }

    /** GET /api/shopping-requests/nearby?lat=&lng= — Shopper tìm đơn gần */
    @GetMapping("/nearby")
    public ResponseEntity<List<ShoppingRequest>> getNearbyRequests(
            @RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(service.getNearbyOpenRequests(lat, lng));
    }

    /** PUT /api/shopping-requests/{id}/accept — Shopper nhận đơn */
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        try {
            Long shopperId = ((Number) body.get("shopperId")).longValue();
            return ResponseEntity.ok(service.acceptRequest(id, shopperId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/shopping-requests/{id}/status — Cập nhật trạng thái */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.updateStatus(id, body.get("status")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/shopping-requests/{id}/items/{itemId} — Shopper cập nhật item */
    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable Long id,
                                         @PathVariable Long itemId,
                                         @RequestBody Map<String, Object> body) {
        try {
            ShoppingRequestItem item = service.updateItem(itemId, body);
            return ResponseEntity.ok(item);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/shopping-requests/{id}/location — Shopper GPS tracking */
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long id,
                                             @RequestBody Map<String, Double> body) {
        try {
            service.updateLocation(id, body.get("shopperLat"), body.get("shopperLng"));
            return ResponseEntity.ok(Map.of("message", "Cập nhật vị trí thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/shopping-requests/{id}/cancel — Hủy đơn */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.cancelRequest(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
