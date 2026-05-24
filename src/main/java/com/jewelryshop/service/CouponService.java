package com.jewelryshop.service;

import com.jewelryshop.entity.Coupon;
import com.jewelryshop.enums.DiscountType;
import com.jewelryshop.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class CouponService {

    @Autowired private CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public Coupon validate(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ!"));

        if (!coupon.isValid()) {
            throw new RuntimeException("Mã giảm giá đã hết hạn hoặc đã dùng hết!");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Đơn hàng tối thiểu " +
                    String.format("%,.0f", coupon.getMinOrderAmount()) + "đ để dùng mã này!");
        }

        return coupon;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            // Ap dung toi da neu co
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        // Giam toi da bang gia tri don hang
        return discount.min(orderAmount);
    }

    public void markUsed(String code) {
        couponRepository.findByCode(code.toUpperCase()).ifPresent(c -> {
            c.setUsedCount(c.getUsedCount() + 1);
            couponRepository.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<Coupon> findAll() { return couponRepository.findAll(); }

    @Transactional(readOnly = true)
    public Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy coupon #" + id));
    }

    public Coupon save(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        if (coupon.getId() != null) {
            Coupon existing = findById(coupon.getId());
            existing.setCode(coupon.getCode());
            existing.setDescription(coupon.getDescription());
            existing.setDiscountType(coupon.getDiscountType());
            existing.setDiscountValue(coupon.getDiscountValue());
            existing.setMinOrderAmount(coupon.getMinOrderAmount());
            existing.setMaxDiscount(coupon.getMaxDiscount());
            existing.setMaxUses(coupon.getMaxUses());
            existing.setActive(coupon.isActive());
            existing.setExpiredAt(coupon.getExpiredAt());
            return couponRepository.save(existing);
        }
        return couponRepository.save(coupon);
    }

    public void delete(Long id) {
        Coupon coupon = findById(id);
        try {
            couponRepository.delete(coupon);
            couponRepository.flush();
        } catch (Exception e) {
            // Fallback to soft delete if referenced by orders (via couponCode text checks, though JPA won't know directly, SQL triggers or constraints might block if custom schema constraint exists)
            coupon.setActive(false);
            couponRepository.save(coupon);
        }
    }
}
