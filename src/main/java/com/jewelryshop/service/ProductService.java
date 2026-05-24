package com.jewelryshop.service;

import com.jewelryshop.entity.Product;
import com.jewelryshop.entity.ProductImage;
import com.jewelryshop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Page<Product> findWithFilters(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String brand, String sort, int page, int size) {
        Sort sortObj = switch (sort == null ? "" : sort) {
            case "price_asc"   -> Sort.by("price").ascending();
            case "price_desc"  -> Sort.by("price").descending();
            case "popular"     -> Sort.by("viewCount").descending();
            default            -> Sort.by("createdAt").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sortObj);
        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim().toLowerCase() + "%" : null;
        String br = (brand   != null && !brand.isBlank())   ? "%" + brand.trim().toLowerCase() + "%"   : null;
        return productRepository.findWithFilters(kw, categoryId, minPrice, maxPrice, br, pageable);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm #" + id));
        // Increment view count
        return p;
    }

    public Product incrementView(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm #" + id));
        p.setViewCount(p.getViewCount() + 1);
        return productRepository.save(p);
    }

    public Product save(Product product, MultipartFile mainImageFile, List<MultipartFile> extraImages)
            throws Exception {
        Product toSave;
        if (product.getId() != null) {
            toSave = findById(product.getId());
            toSave.setName(product.getName());
            toSave.setDescription(product.getDescription());
            toSave.setPrice(product.getPrice());
            toSave.setSalePrice(product.getSalePrice());
            toSave.setStockQuantity(product.getStockQuantity());
            toSave.setBrand(product.getBrand());
            toSave.setMaterial(product.getMaterial());
            toSave.setActive(product.isActive());
            toSave.setCategory(product.getCategory());
        } else {
            toSave = product;
        }

        // Xu ly anh chinh
        if (mainImageFile != null && !mainImageFile.isEmpty()) {
            if (toSave.getMainImage() != null) {
                fileStorageService.deleteFile(toSave.getMainImage());
            }
            String path = fileStorageService.saveFile(mainImageFile);
            toSave.setMainImage(path);
        }

        Product saved = productRepository.save(toSave);

        // Xu ly anh phu
        if (extraImages != null) {
            int order = 0;
            for (MultipartFile img : extraImages) {
                if (img != null && !img.isEmpty()) {
                    String path = fileStorageService.saveFile(img);
                    ProductImage pi = ProductImage.builder()
                            .product(saved)
                            .imagePath(path)
                            .sortOrder(order++)
                            .build();
                    productImageRepository.save(pi);
                }
            }
        }
        return saved;
    }

    public void delete(Long id) {
        Product product = findById(id);
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (Exception e) {
            // Fallback to soft delete if referenced by orders
            product.setActive(false);
            productRepository.save(product);
        }
    }

    public void deleteImage(Long imageId) {
        productImageRepository.findById(imageId).ifPresent(img -> {
            fileStorageService.deleteFile(img.getImagePath());
            productImageRepository.delete(img);
        });
    }

    @Transactional(readOnly = true)
    public List<Product> findNewArrivals() {
        return productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findPopular() {
        return productRepository.findTop8ByActiveTrueOrderByViewCountDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findOnSale() {
        return productRepository.findTop4ByActiveTrueAndSalePriceNotNullOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStock(int threshold) {
        return productRepository.findByStockQuantityLessThanAndActiveTrue(threshold);
    }

    @Transactional(readOnly = true)
    public Page<Product> findAllForAdmin(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return productRepository.countByActiveTrue();
    }

    @Transactional(readOnly = true)
    public long countLowStock(int threshold) {
        return productRepository.countByStockQuantityLessThanAndActiveTrue(threshold);
    }
}
