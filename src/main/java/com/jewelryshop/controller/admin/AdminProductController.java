package com.jewelryshop.controller.admin;

import com.jewelryshop.entity.Product;
import com.jewelryshop.service.CategoryService;
import com.jewelryshop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("products", productService.findAllForAdmin(page, 10));
        model.addAttribute("currentPage", page);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Product product,
                       BindingResult result,
                       @RequestParam(value = "mainImageFile", required = false) MultipartFile mainImageFile,
                       @RequestParam(value = "extraImages", required = false) List<MultipartFile> extraImages,
                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                       RedirectAttributes ra, Model model) {

        if (result.hasErrors()) {
            if (product.getId() != null) {
                try {
                    Product existing = productService.findById(product.getId());
                    product.setImages(existing.getImages());
                } catch (Exception ignored) {}
            }
            model.addAttribute("categories", categoryService.findAll());
            return "admin/products/form";
        }

        try {
            if (categoryId != null) {
                product.setCategory(categoryService.findById(categoryId));
            }
            productService.save(product, mainImageFile, extraImages);
            ra.addFlashAttribute("success", "Lưu sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            productService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa sản phẩm!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/delete-image/{imageId}")
    public String deleteImage(@PathVariable Long imageId,
                              @RequestParam Long productId,
                              RedirectAttributes ra) {
        productService.deleteImage(imageId);
        ra.addFlashAttribute("success", "Đã xóa ảnh!");
        return "redirect:/admin/products/edit/" + productId;
    }
}
