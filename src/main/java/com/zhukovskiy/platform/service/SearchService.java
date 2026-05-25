package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SpecialistProfileRepository specialistProfileRepository;

    /**
     * Поиск специалистов с фильтрацией и сортировкой
     */
    public List<SpecialistProfile> searchSpecialists(
            String category,
            String query,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minRating,
            Integer minExperience,
            String location,
            String sortBy) {

        // Получаем всех активных специалистов (прошедших модерацию)
        List<SpecialistProfile> specialists = specialistProfileRepository.findActiveSpecialists();

        // Полнотекстовый поиск по названиям услуг и описаниям
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            specialists = specialists.stream()
                    .filter(s -> matchesFullTextSearch(s, lowerQuery))
                    .collect(Collectors.toList());
        }

        // Фильтрация по категории
        if (category != null && !category.isEmpty()) {
            specialists = specialists.stream()
                    .filter(s -> s.getCategories() != null && s.getCategories().contains(category))
                    .collect(Collectors.toList());
        }

        // Фильтрация по цене (почасовая или фиксированная)
        if (minPrice != null) {
            specialists = specialists.stream()
                    .filter(s -> (s.getHourlyRate() != null && s.getHourlyRate().compareTo(minPrice) >= 0) ||
                            (s.getFixedPrice() != null && s.getFixedPrice().compareTo(minPrice) >= 0))
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            specialists = specialists.stream()
                    .filter(s -> (s.getHourlyRate() != null && s.getHourlyRate().compareTo(maxPrice) <= 0) ||
                            (s.getFixedPrice() != null && s.getFixedPrice().compareTo(maxPrice) <= 0))
                    .collect(Collectors.toList());
        }

        // Фильтрация по рейтингу
        if (minRating != null && minRating > 0) {
            specialists = specialists.stream()
                    .filter(s -> s.getAverageRating() >= minRating)
                    .collect(Collectors.toList());
        }

        // Фильтрация по опыту работы
        if (minExperience != null && minExperience > 0) {
            specialists = specialists.stream()
                    .filter(s -> s.getExperienceYears() != null && s.getExperienceYears() >= minExperience)
                    .collect(Collectors.toList());
        }

        // Фильтрация по местоположению
        if (location != null && !location.trim().isEmpty()) {
            String lowerLocation = location.toLowerCase().trim();
            specialists = specialists.stream()
                    .filter(s -> s.getServiceArea() != null &&
                            s.getServiceArea().toLowerCase().contains(lowerLocation))
                    .collect(Collectors.toList());
        }

        // Сортировка результатов
        specialists = sortSpecialists(specialists, sortBy);

        return specialists;
    }

    /**
     * Полнотекстовый поиск по названиям услуг и описаниям
     */
    private boolean matchesFullTextSearch(SpecialistProfile specialist, String query) {
        // Поиск в описании
        if (specialist.getDescription() != null &&
                specialist.getDescription().toLowerCase().contains(query)) {
            return true;
        }

        // Поиск в категориях
        if (specialist.getCategories() != null) {
            for (String category : specialist.getCategories()) {
                if (category.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        // Поиск в имени специалиста
        if (specialist.getUser().getFirstName().toLowerCase().contains(query) ||
                specialist.getUser().getLastName().toLowerCase().contains(query)) {
            return true;
        }

        // Поиск в образовании
        if (specialist.getEducation() != null &&
                specialist.getEducation().toLowerCase().contains(query)) {
            return true;
        }

        return false;
    }

    /**
     * Сортировка специалистов
     */
    private List<SpecialistProfile> sortSpecialists(List<SpecialistProfile> specialists, String sortBy) {
        switch (sortBy) {
            case "rating_desc":
                specialists.sort(Comparator.comparing(SpecialistProfile::getAverageRating).reversed());
                break;
            case "rating_asc":
                specialists.sort(Comparator.comparing(SpecialistProfile::getAverageRating));
                break;
            case "price_asc":
                specialists.sort(Comparator.comparing(this::getMinPrice));
                break;
            case "price_desc":
                specialists.sort(Comparator.comparing(this::getMinPrice).reversed());
                break;
            case "experience_desc":
                specialists.sort(Comparator.comparing(s -> s.getExperienceYears() != null ?
                        s.getExperienceYears() : 0, Comparator.reverseOrder()));
                break;
            case "experience_asc":
                specialists.sort(Comparator.comparing(s -> s.getExperienceYears() != null ?
                        s.getExperienceYears() : 0));
                break;
            case "distance_asc":
                // Для реального расстояния нужна геолокация
                // Пока сортируем по умолчанию
                specialists.sort(Comparator.comparing(SpecialistProfile::getAverageRating).reversed());
                break;
            default:
                specialists.sort(Comparator.comparing(SpecialistProfile::getAverageRating).reversed());
                break;
        }
        return specialists;
    }

    /**
     * Получение минимальной цены (для сортировки)
     */
    private BigDecimal getMinPrice(SpecialistProfile specialist) {
        if (specialist.getHourlyRate() != null) {
            return specialist.getHourlyRate();
        }
        if (specialist.getFixedPrice() != null) {
            return specialist.getFixedPrice();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Поиск специалистов поблизости (упрощенная версия)
     */
    public List<SpecialistProfile> findSpecialistsNearby(String address, int radiusKm) {
        List<SpecialistProfile> allSpecialists = specialistProfileRepository.findActiveSpecialists();

        String lowerAddress = address.toLowerCase().trim();
        return allSpecialists.stream()
                .filter(s -> s.getServiceArea() != null &&
                        s.getServiceArea().toLowerCase().contains(lowerAddress))
                .collect(Collectors.toList());
    }

    /**
     * Получение доступных фильтров (для отображения в UI)
     */
    public SearchFilters getAvailableFilters() {
        return SearchFilters.builder()
                .categories(List.of(
                        "Сантехника", "Электрика", "Ремонт квартир", "Уборка",
                        "Переезды", "Ремонт техники", "Садоводство", "Репетиторство",
                        "Фотография", "Дизайн", "Строительство", "Клининг",
                        "Грузоперевозки", "Красота и здоровье", "IT и программирование"
                ))
                .priceRange(PriceRange.builder().min(0).max(500).build())
                .ratingRange(RatingRange.builder().min(0).max(5).step(0.5).build())
                .experienceRange(ExperienceRange.builder().min(0).max(30).build())
                .sortOptions(List.of(
                        new SortOption("rating_desc", "По рейтингу (высокий → низкий)"),
                        new SortOption("rating_asc", "По рейтингу (низкий → высокий)"),
                        new SortOption("price_asc", "По цене (дешевые → дорогие)"),
                        new SortOption("price_desc", "По цене (дорогие → дешевые)"),
                        new SortOption("experience_desc", "По опыту (больше → меньше)"),
                        new SortOption("experience_asc", "По опыту (меньше → больше)"),
                        new SortOption("distance_asc", "По расстоянию")
                ))
                .build();
    }

    // Вспомогательные классы для фильтров
    @lombok.Data
    @lombok.Builder
    public static class SearchFilters {
        private List<String> categories;
        private PriceRange priceRange;
        private RatingRange ratingRange;
        private ExperienceRange experienceRange;
        private List<SortOption> sortOptions;
    }

    @lombok.Data
    @lombok.Builder
    public static class PriceRange {
        private int min;
        private int max;
    }

    @lombok.Data
    @lombok.Builder
    public static class RatingRange {
        private int min;
        private int max;
        private double step;
    }

    @lombok.Data
    @lombok.Builder
    public static class ExperienceRange {
        private int min;
        private int max;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SortOption {
        private String value;
        private String label;
    }
}