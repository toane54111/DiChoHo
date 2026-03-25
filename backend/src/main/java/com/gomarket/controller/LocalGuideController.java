package com.gomarket.controller;

import com.gomarket.dto.LocalGuideResponse;
import com.gomarket.service.LocalGuideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Thổ Địa — Gợi ý đặc sản theo mùa vụ, vị trí và khẩu vị
 */
@RestController
@RequestMapping("/api/local-guide")
@CrossOrigin(origins = "*")
public class LocalGuideController {

    private final LocalGuideService localGuideService;

    public LocalGuideController(LocalGuideService localGuideService) {
        this.localGuideService = localGuideService;
    }

    /**
     * GET /api/local-guide/suggestions?userId=1&lat=10.77&lng=106.70
     * Trả về gợi ý cá nhân hóa cho user tại vị trí hiện tại
     */
    @GetMapping("/suggestions")
    public ResponseEntity<LocalGuideResponse> getSuggestions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10.7769") double lat,
            @RequestParam(defaultValue = "106.7009") double lng) {
        return ResponseEntity.ok(localGuideService.getSuggestions(userId, lat, lng));
    }

    /**
     * POST /api/local-guide/update-taste — Cập nhật hồ sơ khẩu vị từ lịch sử
     */
    @PostMapping("/update-taste")
    public ResponseEntity<?> updateTasteProfile(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId required"));
        }
        localGuideService.updateTasteProfileFromHistory(userId);
        return ResponseEntity.ok(Map.of("message", "Taste profile updated"));
    }
}
