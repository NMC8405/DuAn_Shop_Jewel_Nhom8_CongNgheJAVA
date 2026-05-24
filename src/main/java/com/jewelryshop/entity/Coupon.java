package com.jewelryshop.entity;

import com.jewelryshop.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã coupon không được để trống")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    @Builder.Default
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @DecimalMin(value = "0.01", message = "Giá trị giảm phải > 0")
    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 18, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "max_uses")
    @Builder.Default
    private int maxUses = 100;

    @Column(name = "used_count")
    @Builder.Default
    private int usedCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "expired_at")
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public boolean isValid() {
        return active
            && usedCount < maxUses
            && (expiredAt == null || expiredAt.isAfter(LocalDateTime.now()));
    }
}
