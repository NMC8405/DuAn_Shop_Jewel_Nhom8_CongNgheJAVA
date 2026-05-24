package com.jewelryshop.controller.admin;
import com.jewelryshop.entity.Coupon;
import com.jewelryshop.enums.DiscountType;
import com.jewelryshop.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {
    @Autowired private CouponService couponService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("coupons", couponService.findAll());
        return "admin/coupons/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("coupon", new Coupon());
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/coupons/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("coupon", couponService.findById(id));
        model.addAttribute("discountTypes", DiscountType.values());
        return "admin/coupons/form";
    }

    @PostMapping("/save")
    public String save(@jakarta.validation.Valid @ModelAttribute Coupon coupon,
                       org.springframework.validation.BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("discountTypes", DiscountType.values());
            return "admin/coupons/form";
        }
        try {
            couponService.save(coupon);
            ra.addFlashAttribute("success", "Lưu mã giảm giá thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi lưu mã giảm giá: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            couponService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa mã giảm giá!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xóa mã giảm giá: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
