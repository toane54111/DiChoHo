package com.gomarket.repository;

import com.gomarket.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    // Thống kê chi tiêu theo tháng: [tháng, tổng chi]
    @Query("SELECT MONTH(t.createdAt), SUM(ABS(t.amount)) FROM WalletTransaction t " +
           "WHERE t.walletId = :wid AND t.type = 'PAYMENT' AND YEAR(t.createdAt) = :year " +
           "GROUP BY MONTH(t.createdAt) ORDER BY MONTH(t.createdAt)")
    List<Object[]> getMonthlySpending(@Param("wid") Long walletId, @Param("year") int year);
}
