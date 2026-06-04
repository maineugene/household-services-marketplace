package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.PortfolioItemDto;
import com.zhukovskiy.platform.dto.SpecialistProfileDto;
import com.zhukovskiy.platform.model.PortfolioItem;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.security.SecurityUtils;
import com.zhukovskiy.platform.service.PortfolioService;
import com.zhukovskiy.platform.service.SpecialistProfileService;
import com.zhukovskiy.platform.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/specialist")
@RequiredArgsConstructor
public class SpecialistProfileController {

    private final SpecialistProfileService specialistProfileService;
    private final PortfolioService portfolioService;
    private final SecurityUtils securityUtils;

    /**
     * Страница создания/редактирования профиля специалиста
     */
    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        User currentUser = securityUtils.getCurrentUser();

        try {
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);
            SpecialistProfileDto profileDto = specialistProfileService.convertToDto(profile);
            model.addAttribute("profile", profileDto);
            model.addAttribute("isEdit", true);
        } catch (ResourceNotFoundException e) {
            model.addAttribute("profile", new SpecialistProfileDto());
            model.addAttribute("isEdit", false);
        }

        model.addAttribute("categories", getAvailableCategories());
        return "specialist/profile-form";
    }

    /**
     * Сохранение профиля специалиста
     */
    @PostMapping("/profile/save")
    public String saveProfile(@Valid @ModelAttribute("profile") SpecialistProfileDto profileDto,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", getAvailableCategories());
            return "specialist/profile-form";
        }

        try {
            User currentUser = securityUtils.getCurrentUser();
            specialistProfileService.createOrUpdateProfile(currentUser, profileDto);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно сохранен и отправлен на модерацию");
            return "redirect:/specialist/profile/view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при сохранении профиля: " + e.getMessage());
            return "redirect:/specialist/profile/edit";
        }
    }

    /**
     * Просмотр профиля специалиста
     */
    @GetMapping("/profile/view")
    public String viewProfile(Model model) {
        User currentUser = securityUtils.getCurrentUser();

        try {
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);
            model.addAttribute("profile", profile);
            model.addAttribute("portfolio", portfolioService.getPortfolioBySpecialist(profile));

            // Получаем статистику рейтинга
            var ratingStats = specialistProfileService.getRatingStatistics(currentUser);
            model.addAttribute("ratingStats", ratingStats);

            return "specialist/profile-view";
        } catch (ResourceNotFoundException e) {
            return "redirect:/specialist/profile/edit";
        }
    }

    /**
     * Публичный просмотр профиля специалиста (для заказчиков)
     */
    @GetMapping("/public/{id}")
    public String viewPublicProfile(@PathVariable Long id, Model model) {
        try {
            SpecialistProfile profile = specialistProfileService.getProfileById(id);
            model.addAttribute("profile", profile);
            model.addAttribute("portfolio", portfolioService.getPortfolioBySpecialist(profile));
            model.addAttribute("reviews", specialistProfileService.getApprovedReviews(profile.getUser()));

            return "specialist/public-profile";
        } catch (ResourceNotFoundException e) {
            return "error/404";
        }
    }

    /**
     * Добавление работы в портфолио
     */
    @PostMapping("/portfolio/add")
    public String addPortfolioItem(@RequestParam("title") String title,
                                   @RequestParam("description") String description,
                                   @RequestParam("image") MultipartFile image,
                                   RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);

            // Проверяем количество фотографий (максимум 20)
            int currentCount = portfolioService.getPortfolioCount(profile);
            if (currentCount >= 20) {
                redirectAttributes.addFlashAttribute("error", "Достигнут лимит фотографий в портфолио (максимум 20)");
                return "redirect:/specialist/profile/view";
            }

            PortfolioItemDto portfolioDto = PortfolioItemDto.builder()
                    .title(title)
                    .description(description)
                    .build();

            portfolioService.addPortfolioItem(profile, portfolioDto, image);
            redirectAttributes.addFlashAttribute("success", "Работа успешно добавлена в портфолио");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при добавлении: " + e.getMessage());
        }

        return "redirect:/specialist/profile/view";
    }

    /**
     * Удаление работы из портфолио
     */
    @PostMapping("/portfolio/delete/{itemId}")
    public String deletePortfolioItem(@PathVariable Long itemId, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);
            portfolioService.deletePortfolioItem(profile, itemId);
            redirectAttributes.addFlashAttribute("success", "Работа удалена из портфолио");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении: " + e.getMessage());
        }

        return "redirect:/specialist/profile/view";
    }

    /**
     * Редактирование услуг и цен
     */
    @PostMapping("/services/update")
    public String updateServices(@RequestParam(required = false) String hourlyRate,
                                 @RequestParam(required = false) String fixedPrice,
                                 @RequestParam("categories") List<String> categories,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            SpecialistProfile profile = specialistProfileService.getProfileByUser(currentUser);

            profile.setHourlyRate(hourlyRate != null && !hourlyRate.isEmpty() ?
                    new BigDecimal(hourlyRate) : null);
            profile.setFixedPrice(fixedPrice != null && !fixedPrice.isEmpty() ?
                    new BigDecimal(fixedPrice) : null);
            profile.setCategories(categories);

            specialistProfileService.updateProfile(profile);
            redirectAttributes.addFlashAttribute("success", "Услуги и цены успешно обновлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
        }

        return "redirect:/specialist/profile/view";
    }

    private List<String> getAvailableCategories() {
        return List.of(
                "Сантехника",
                "Электрика",
                "Ремонт квартир",
                "Уборка",
                "Переезды",
                "Ремонт техники",
                "Садоводство",
                "Репетиторство",
                "Фотография",
                "Дизайн"
        );
    }
}