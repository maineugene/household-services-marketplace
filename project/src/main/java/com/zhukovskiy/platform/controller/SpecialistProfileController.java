package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.SpecialistProfileDto;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.security.SecurityUtils;
import com.zhukovskiy.platform.service.SpecialistProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/specialist")
@RequiredArgsConstructor
public class SpecialistProfileController {

    private final SpecialistProfileService specialistProfileService;
    private final SecurityUtils securityUtils;

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        User currentUser = securityUtils.getCurrentUser();

        try {
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);
            SpecialistProfileDto profileDto = specialistProfileService.convertToDto(profile);
            model.addAttribute("profile", profileDto);
            model.addAttribute("isEdit", true);
        } catch (RuntimeException e) {
            model.addAttribute("profile", new SpecialistProfileDto());
            model.addAttribute("isEdit", false);
        }

        model.addAttribute("categories", specialistProfileService.getAllCategories());
        return "specialist/profile-form";
    }

    @PostMapping("/profile/save")
    public String saveProfile(@Valid @ModelAttribute("profile") SpecialistProfileDto profileDto,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("categories", specialistProfileService.getAllCategories());
            return "specialist/profile-form";
        }

        try {
            User currentUser = securityUtils.getCurrentUser();
            specialistProfileService.createOrUpdateProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно сохранен и отправлен на модерацию");
            return "redirect:/specialist/profile/view";
        } catch (Exception e) {
            e.printStackTrace(); // Для отладки - посмотрите в консоли ошибку
            redirectAttributes.addFlashAttribute("error", "Ошибка при сохранении профиля: " + e.getMessage());
            return "redirect:/specialist/profile/edit";
        }
    }

    @GetMapping("/profile/view")
    public String viewProfile(Model model) {
        User currentUser = securityUtils.getCurrentUser();

        try {
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);
            model.addAttribute("profile", profile);
            return "specialist/profile-view";
        } catch (RuntimeException e) {
            return "redirect:/specialist/profile/edit";
        }
    }
}