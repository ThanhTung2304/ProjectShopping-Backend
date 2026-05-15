package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    //Tìm coupon hợp lệ: đang active, còn hạn, còn lượt dùng
    @Query("SELECT c FROM Coupon c WHERE c.code = :code" +
            " AND c.isActive = true" +
            " AND c.startDate <= :today" +
            " AND c.endDate >= :today" +
            " AND c.usedCount < c.usageLimit")
    Optional<Coupon> findValidCoupon(String code, LocalDate today);

    // Tăng used_count khi áp dụng coupon thành công
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id")
    void incrementUsedCount(Long id);
}
