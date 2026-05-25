package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.service.SearchService;
import com.zhukovskiy.platform.service.SpecialistProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SpecialistProfileService specialistProfileService;

    /**
     * Страница поиска специалистов с фильтрацией
     */
    @GetMapping("/specialists")
    public String searchSpecialists(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "rating_desc") String sortBy,
            Model model) {

        // Выполняем поиск
        List<SpecialistProfile> specialists = searchService.searchSpecialists(
                category, query, minPrice, maxPrice, minRating, minExperience, location, sortBy);

        // Получаем доступные фильтры
        SearchService.SearchFilters filters = searchService.getAvailableFilters();

        // Добавляем атрибуты в модель
        model.addAttribute("specialists", specialists);
        model.addAttribute("categories", filters.getCategories());
        model.addAttribute("priceRange", filters.getPriceRange());
        model.addAttribute("ratingRange", filters.getRatingRange());
        model.addAttribute("experienceRange", filters.getExperienceRange());
        model.addAttribute("sortOptions", filters.getSortOptions());

        // Сохраняем текущие параметры поиска для отображения в форме
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentQuery", query);
        model.addAttribute("currentMinPrice", minPrice);
        model.addAttribute("currentMaxPrice", maxPrice);
        model.addAttribute("currentMinRating", minRating);
        model.addAttribute("currentMinExperience", minExperience);
        model.addAttribute("currentLocation", location);
        model.addAttribute("currentSortBy", sortBy);

        // Статистика поиска
        model.addAttribute("totalResults", specialists.size());

        return "search/results";
    }

    /**
     * Быстрый поиск по местоположению
     */
    @GetMapping("/nearby")
    public String searchNearby(@RequestParam(required = false) String address,
                               @RequestParam(defaultValue = "10") Integer radiusKm,
                               Model model) {
        if (address == null || address.trim().isEmpty()) {
            return "search/location-search";
        }

        List<SpecialistProfile> specialists = searchService.findSpecialistsNearby(address, radiusKm);

        model.addAttribute("specialists", specialists);
        model.addAttribute("address", address);
        model.addAttribute("radius", radiusKm);
        model.addAttribute("totalResults", specialists.size());

        return "search/nearby-results";
    }

    /**
     * Главная страница поиска (с формой)
     */
    @GetMapping
    public String searchPage(Model model) {
        SearchService.SearchFilters filters = searchService.getAvailableFilters();
        model.addAttribute("categories", filters.getCategories());
        model.addAttribute("priceRange", filters.getPriceRange());
        return "search/index";
    }
}