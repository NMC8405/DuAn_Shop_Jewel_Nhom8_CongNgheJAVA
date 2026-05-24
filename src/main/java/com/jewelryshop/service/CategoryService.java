package com.jewelryshop.service;

import com.jewelryshop.entity.Category;
import com.jewelryshop.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CategoryService {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryRepository.findByActiveTrueOrderByName();
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục #" + id));
    }

    public Category save(Category category) {
        if (category.getId() != null) {
            Category existing = findById(category.getId());
            existing.setName(category.getName());
            existing.setDescription(category.getDescription());
            existing.setActive(category.isActive());
            return categoryRepository.save(existing);
        }
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        Category cat = findById(id);
        try {
            categoryRepository.delete(cat);
            categoryRepository.flush();
        } catch (Exception e) {
            // Fallback to soft delete if it has products referencing it
            cat.setActive(false);
            categoryRepository.save(cat);
        }
    }
}
