package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.service.SearchService;
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

    @GetMapping
    public String searchPage(Model model) {
        model.addAttribute("categories", getCategories());
        return "search/index";
    }

    @GetMapping("/results")
    public String searchResults(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "rating_desc") String sortBy,
            Model model) {

        List<SpecialistProfile> specialists = searchService.searchSpecialists(
                category, query, minPrice, maxPrice, minRating, minExperience, location, sortBy);

        model.addAttribute("specialists", specialists);
        model.addAttribute("categories", getCategories());
        model.addAttribute("totalResults", specialists.size());
        return "search/results";
    }

    private List<String> getCategories() {
        return List.of("Сантехника", "Электрика", "Ремонт", "Уборка", "Переезды");
    }
}