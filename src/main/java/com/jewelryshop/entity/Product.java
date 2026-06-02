package com.jewelryshop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0", message = "Giá phải >= 0")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 18, scale = 2)
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String material;

    @Min(value = 0, message = "Tồn kho phải >= 0")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(name = "main_image", length = 255)
    private String mainImage;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper: lay gia hien tai (sale hoac binh thuong)
    public BigDecimal getCurrentPrice() {
        return (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) ? salePrice : price;
    }

    // Helper: co sale khong
    public boolean isOnSale() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(price) < 0;
    }
}
