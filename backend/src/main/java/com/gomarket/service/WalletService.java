package com.gomarket.service;

import com.gomarket.model.Wallet;
import com.gomarket.model.WalletTransaction;
import com.gomarket.repository.WalletRepository;
import com.gomarket.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    /** Lấy ví, tạo mới nếu chưa có */
    @Transactional
    public Wallet getOrCreate(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    public Long getBalance(Long userId) {
        return getOrCreate(userId).getBalance();
    }

    /** Nạp tiền vào ví */
    @Transactional
    public Wallet topUp(Long userId, Long amount) {
        if (amount <= 0) throw new RuntimeException("Số tiền nạp phải lớn hơn 0");

        Wallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "TOP_UP", amount,
                "Nạp " + String.format("%,d", amount) + "đ vào ví", null));

        return wallet;
    }

    /**
     * Trừ tiền thanh toán đơn hàng.
     * Dùng PESSIMISTIC_WRITE để tránh race condition khi 2 request đồng thời.
     */
    @Transactional
    public void pay(Long userId, Long orderId, Long amount) {
        // Dùng lock để tránh race condition
        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        if (wallet.getBalance() < amount)
            throw new RuntimeException("Số dư ví không đủ. Cần: " +
                    String.format("%,d", amount) + "đ, hiện có: " +
                    String.format("%,d", wallet.getBalance()) + "đ");

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        String desc = orderId != null
                ? "Thanh toán đơn hàng #" + orderId
                : "Thanh toán đơn hàng";
        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "PAYMENT", -amount, desc, orderId));
    }

    /** Đóng băng tiền khi tạo đơn (trừ trước, hoàn lại nếu hủy) */
    @Transactional
    public void freeze(Long userId, Long orderId, Long amount) {
        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        if (wallet.getBalance() < amount)
            throw new RuntimeException("Số dư ví không đủ. Cần: " +
                    String.format("%,d", amount) + "đ, hiện có: " +
                    String.format("%,d", wallet.getBalance()) + "đ");

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "FREEZE", -amount,
                "Đóng băng cho đơn #" + orderId, orderId));
    }

    /** Hoàn tiền đóng băng khi hủy đơn */
    @Transactional
    public void unfreeze(Long userId, Long orderId, Long amount) {
        Wallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "UNFREEZE", amount,
                "Hoàn tiền đóng băng đơn #" + orderId, orderId));
    }

    /** Chuyển phí đi chợ cho shopper */
    @Transactional
    public void creditShopper(Long shopperId, Long orderId, Long amount) {
        Wallet wallet = getOrCreate(shopperId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "EARNING", amount,
                "Phí đi chợ đơn #" + orderId, orderId));
    }

    /** Trừ thêm khi vượt ngân sách */
    @Transactional
    public void chargeExtra(Long userId, Long orderId, Long extraAmount) {
        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        if (wallet.getBalance() < extraAmount)
            throw new RuntimeException("Số dư ví không đủ để trả phần vượt ngân sách");

        wallet.setBalance(wallet.getBalance() - extraAmount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "EXTRA_CHARGE", -extraAmount,
                "Phí vượt ngân sách đơn #" + orderId, orderId));
    }

    /** Hoàn tiền khi hủy đơn */
    @Transactional
    public void refund(Long userId, Long orderId, Long amount) {
        Wallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet.getId(), "REFUND", amount,
                "Hoàn tiền đơn hàng #" + orderId, orderId));
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        Wallet wallet = getOrCreate(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    public List<Object[]> getMonthlyStats(Long userId, int year) {
        Wallet wallet = getOrCreate(userId);
        return transactionRepository.getMonthlySpending(wallet.getId(), year);
    }

    /**
     * Sinh QR payload (giả lập cho đồ án, không có chữ ký bảo mật).
     * Format: DICHO_PAY:userId=1:amount=500000:ref=TXN1741867200
     * ⚠️ Production: cần thêm sig=SHA256(payload + SECRET_KEY)
     */
    public String generateQrPayload(Long userId, Long amount) {
        long ref = System.currentTimeMillis() / 1000;
        return "DICHO_PAY:userId=" + userId + ":amount=" + amount + ":ref=TXN" + ref;
    }

    /** Xử lý QR scan → nạp tiền vào ví */
    @Transactional
    public Wallet processQrPayload(String payload) {
        // Parse: DICHO_PAY:userId=1:amount=500000:ref=TXN...
        if (!payload.startsWith("DICHO_PAY:"))
            throw new RuntimeException("QR không hợp lệ");

        String[] parts = payload.split(":");
        Long userId = null;
        Long amount = null;

        for (String part : parts) {
            if (part.startsWith("userId=")) userId = Long.parseLong(part.substring(7));
            if (part.startsWith("amount=")) amount = Long.parseLong(part.substring(7));
        }

        if (userId == null || amount == null)
            throw new RuntimeException("QR bị thiếu thông tin");

        return topUp(userId, amount);
    }
}
