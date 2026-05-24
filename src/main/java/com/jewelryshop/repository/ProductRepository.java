package com.jewelryshop.repository;

import com.jewelryshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:brand IS NULL OR LOWER(p.brand) LIKE :brand)")
    Page<Product> findWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("brand") String brand,
            Pageable pageable);

    List<Product> findTop8ByActiveTrueOrderByCreatedAtDesc();
    List<Product> findTop8ByActiveTrueOrderByViewCountDesc();
    List<Product> findTop4ByActiveTrueAndSalePriceNotNullOrderByCreatedAtDesc();
    List<Product> findByStockQuantityLessThanAndActiveTrue(int threshold);
    long countByActiveTrue();
    long countByStockQuantityLessThanAndActiveTrue(int threshold);
}
