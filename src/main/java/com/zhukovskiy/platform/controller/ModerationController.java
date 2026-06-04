package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.model.ModerationStatus;
import com.zhukovskiy.platform.model.Review;
import com.zhukovskiy.platform.model.ReviewStatus;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.security.SecurityUtils;
import com.zhukovskiy.platform.service.ReviewService;
import com.zhukovskiy.platform.service.SpecialistProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/moderation")
@PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class ModerationController {

    private final SpecialistProfileService specialistProfileService;
    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    /**
     * Панель модератора
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingProfiles", specialistProfileService.getProfilesForModeration());
        model.addAttribute("pendingReviews", reviewService.getPendingReviews());
        model.addAttribute("moderator", securityUtils.getCurrentUser());
        return "moderation/dashboard";
    }

    /**
     * Просмотр профиля на модерации
     */
    @GetMapping("/profile/{id}")
    public String viewProfileForModeration(@PathVariable Long id, Model model) {
        SpecialistProfile profile = specialistProfileService.getProfileById(id);
        model.addAttribute("profile", profile);
        model.addAttribute("portfolio", profile.getPortfolio());
        return "moderation/profile-review";
    }

    /**
     * Одобрение профиля специалиста
     */
    @PostMapping("/profile/approve/{id}")
    public String approveProfile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            specialistProfileService.moderateProfile(id, ModerationStatus.APPROVED);
            redirectAttributes.addFlashAttribute("success", "Профиль специалиста одобрен");
        } catch (Exception e) {
            log.error("Ошибка при одобрении профиля", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при одобрении профиля");
        }
        return "redirect:/moderation/dashboard";
    }

    /**
     * Отклонение профиля специалиста
     */
    @PostMapping("/profile/reject/{id}")
    public String rejectProfile(@PathVariable Long id,
                                @RequestParam String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            specialistProfileService.moderateProfile(id, ModerationStatus.REJECTED);
            redirectAttributes.addFlashAttribute("warning", "Профиль отклонен. Причина: " + reason);
        } catch (Exception e) {
            log.error("Ошибка при отклонении профиля", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при отклонении профиля");
        }
        return "redirect:/moderation/dashboard";
    }

    /**
     * Просмотр отзыва на модерации
     */
    @GetMapping("/review/{id}")
    public String viewReviewForModeration(@PathVariable Long id, Model model) {
        Review review = reviewService.getReviewById(id);
        model.addAttribute("review", review);
        model.addAttribute("order", review.getOrder());
        return "moderation/review-review";
    }

    /**
     * Одобрение отзыва
     */
    @PostMapping("/review/approve/{id}")
    public String approveReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.moderateReview(id, ReviewStatus.APPROVED, null);
            redirectAttributes.addFlashAttribute("success", "Отзыв одобрен и опубликован");
        } catch (Exception e) {
            log.error("Ошибка при одобрении отзыва", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при одобрении отзыва");
        }
        return "redirect:/moderation/dashboard";
    }

    /**
     * Отклонение отзыва
     */
    @PostMapping("/review/reject/{id}")
    public String rejectReview(@PathVariable Long id,
                               @RequestParam String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.moderateReview(id, ReviewStatus.REJECTED, reason);
            redirectAttributes.addFlashAttribute("warning", "Отзыв отклонен. Причина: " + reason);
        } catch (Exception e) {
            log.error("Ошибка при отклонении отзыва", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при отклонении отзыва");
        }
        return "redirect:/moderation/dashboard";
    }
}