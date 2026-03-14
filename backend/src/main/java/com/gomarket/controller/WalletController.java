package com.gomarket.controller;

import com.gomarket.model.Wallet;
import com.gomarket.model.WalletTransaction;
import com.gomarket.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    /** GET /api/wallet/{userId} — Lấy thông tin ví */
    @GetMapping("/{userId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getOrCreate(userId));
    }

    /** POST /api/wallet/{userId}/topup — Nạp tiền */
    @PostMapping("/{userId}/topup")
    public ResponseEntity<?> topUp(@PathVariable Long userId,
                                   @RequestBody Map<String, Long> body) {
        try {
            Long amount = body.get("amount");
            if (amount == null || amount <= 0)
                return ResponseEntity.badRequest().body("Số tiền không hợp lệ");
            Wallet wallet = walletService.topUp(userId, amount);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/wallet/{userId}/transactions — Lịch sử giao dịch */
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }

    /** GET /api/wallet/{userId}/stats?year=2025 — Thống kê chi tiêu theo tháng */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<List<Object[]>> getStats(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(walletService.getMonthlyStats(userId, year));
    }

    /** POST /api/wallet/qr/generate — Sinh mã QR thanh toán */
    @PostMapping("/qr/generate")
    public ResponseEntity<Map<String, String>> generateQr(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        Long amount = body.get("amount");
        if (userId == null || amount == null)
            return ResponseEntity.badRequest().build();
        String payload = walletService.generateQrPayload(userId, amount);
        return ResponseEntity.ok(Map.of("payload", payload, "generatedAt", LocalDateTime.now().toString()));
    }

    /** POST /api/wallet/qr/process — Xử lý scan QR → nạp tiền */
    @PostMapping("/qr/process")
    public ResponseEntity<?> processQr(@RequestBody Map<String, String> body) {
        try {
            String payload = body.get("payload");
            Wallet wallet = walletService.processQrPayload(payload);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
