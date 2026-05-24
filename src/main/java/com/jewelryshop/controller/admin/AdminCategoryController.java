package com.jewelryshop.controller.admin;
import com.jewelryshop.entity.Category;
import com.jewelryshop.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    @Autowired private CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "admin/categories/form";
    }

    @PostMapping("/save")
    public String save(@jakarta.validation.Valid @ModelAttribute Category category,
                       org.springframework.validation.BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "admin/categories/form";
        }
        try {
            categoryService.save(category);
            ra.addFlashAttribute("success", "Lưu danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi lưu danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa danh mục!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xóa danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
