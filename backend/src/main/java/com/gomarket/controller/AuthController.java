package com.gomarket.controller;

import com.gomarket.dto.LoginRequest;
import com.gomarket.dto.RegisterRequest;
import com.gomarket.dto.UserResponse;
import com.gomarket.model.User;
import com.gomarket.repository.UserRepository;
import com.gomarket.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            UserResponse user = authService.login(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        try {
            UserResponse user = authService.getProfile(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse user = authService.register(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/auth/{userId}/online-status — Toggle shopper online/offline */
    @PutMapping("/{userId}/online-status")
    public ResponseEntity<?> updateOnlineStatus(@PathVariable Long userId,
                                                 @RequestBody Map<String, Boolean> body) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            user.setIsOnline(body.get("isOnline"));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /api/auth/{userId}/location — Cập nhật vị trí shopper */
    @PutMapping("/{userId}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long userId,
                                             @RequestBody Map<String, Double> body) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            user.setLatitude(body.get("latitude"));
            user.setLongitude(body.get("longitude"));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Cập nhật vị trí thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/auth/shoppers/nearby?lat=&lng= — Tìm shopper online gần đây */
    @GetMapping("/shoppers/nearby")
    public ResponseEntity<?> getNearbyShoppers(@RequestParam double lat,
                                                @RequestParam double lng) {
        List<User> shoppers = userRepository.findNearbyShoppers(lat, lng, 15.0);
        List<UserResponse> responses = shoppers.stream()
                .map(s -> UserResponse.fromUser(s, null))
                .toList();
        return ResponseEntity.ok(responses);
    }

    /** PUT /api/auth/{userId}/profile — Cập nhật profile */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId,
                                            @RequestBody Map<String, String> body) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            if (body.containsKey("bio")) user.setBio(body.get("bio"));
            if (body.containsKey("vehicleType")) user.setVehicleType(body.get("vehicleType"));
            if (body.containsKey("avatarUrl")) user.setAvatarUrl(body.get("avatarUrl"));
            if (body.containsKey("fullName")) user.setFullName(body.get("fullName"));
            userRepository.save(user);
            return ResponseEntity.ok(UserResponse.fromUser(user, null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
