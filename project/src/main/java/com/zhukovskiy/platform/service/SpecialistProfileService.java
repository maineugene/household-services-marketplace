package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.SpecialistProfileDto;
import com.zhukovskiy.platform.mapper.SpecialistMapper;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.ReviewRepository;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialistProfileService {

    private final SpecialistProfileRepository specialistProfileRepository;
    private final UserRepository userRepository;
    private final SpecialistMapper specialistMapper;
    private final ReviewRepository reviewRepository;

    /**
     * Создание или обновление профиля специалиста
     */
    @Transactional
    public SpecialistProfile createOrUpdateProfile(User user, SpecialistProfileDto profileDto) {
        // Проверяем, что пользователь имеет роль SPECIALIST
        if (user.getRole() != Role.SPECIALIST) {
            throw new AccessDeniedException("Только специалисты могут иметь профиль");
        }

        // Ищем существующий профиль
        SpecialistProfile profile = specialistProfileRepository.findByUser(user).orElse(null);

        if (profile == null) {
            // Создаем новый профиль
            profile = specialistMapper.toEntity(profileDto);
            profile.setUser(user);
            profile.setModerationStatus(ModerationStatus.PENDING);
            profile.setIsVerified(false);
            profile.setAverageRating(0.0);
        } else {
            // Обновляем существующий профиль
            profile.setDescription(profileDto.getDescription());
            profile.setExperienceYears(profileDto.getExperienceYears());
            profile.setEducation(profileDto.getEducation());
            profile.setCategories(profileDto.getCategories());
            profile.setHourlyRate(profileDto.getHourlyRate());
            profile.setFixedPrice(profileDto.getFixedPrice());
            profile.setServiceArea(profileDto.getServiceArea());
            // При изменении основных данных отправляем на модерацию
            profile.setModerationStatus(ModerationStatus.PENDING);
            profile.setIsVerified(false);
        }

        return specialistProfileRepository.save(profile);
    }

    /**
     * Получение профиля специалиста по пользователю
     */
    public SpecialistProfile getProfileByUser(User user) {
        return specialistProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Профиль не найден"));
    }

    /**
     * Получение профиля по ID
     */
    public SpecialistProfile getProfileById(Long id) {
        return specialistProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Профиль не найден"));
    }

    /**
     * Получение всех активных специалистов (прошедших модерацию)
     */
    public List<SpecialistProfile> getActiveSpecialists() {
        return specialistProfileRepository.findActiveSpecialists();
    }

    /**
     * Поиск специалистов по категории
     */
    public List<SpecialistProfile> findSpecialistsByCategory(String category) {
        return specialistProfileRepository.findByCategories(List.of(category));
    }

    /**
     * Получение профилей на модерации (для модератора)
     */
    public List<SpecialistProfile> getProfilesForModeration() {
        return specialistProfileRepository.findByModerationStatus(ModerationStatus.PENDING);
    }

    /**
     * Модерация профиля специалиста (для модератора/админа)
     */
    @Transactional
    public void moderateProfile(Long profileId, ModerationStatus status) {
        SpecialistProfile profile = specialistProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Профиль не найден"));

        profile.setModerationStatus(status);
        if (status == ModerationStatus.APPROVED) {
            profile.setIsVerified(true);
        }
        specialistProfileRepository.save(profile);
    }

    /**
     * Обновление рейтинга специалиста
     */
    @Transactional
    public void updateRating(User specialist) {
        Double averageRating = specialistProfileRepository.calculateAverageRatingForSpecialist(specialist);
        SpecialistProfile profile = getProfileByUser(specialist);
        profile.setAverageRating(averageRating != null ? averageRating : 0.0);
        specialistProfileRepository.save(profile);
    }

    /**
     * Обновление профиля (для редактирования услуг и цен)
     */
    @Transactional
    public SpecialistProfile updateProfile(SpecialistProfile profile) {
        return specialistProfileRepository.save(profile);
    }

    /**
     * Конвертация сущности в DTO
     */
    public SpecialistProfileDto convertToDto(SpecialistProfile profile) {
        return specialistMapper.toDto(profile);
    }

    /**
     * Получение всех категорий услуг
     */
    public List<String> getAllCategories() {
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
                "Дизайн",
                "Строительство",
                "Клининг",
                "Грузоперевозки",
                "Красота и здоровье",
                "IT и программирование"
        );
    }

    /**
     * Получение одобренных отзывов о специалисте
     */
    public List<Review> getApprovedReviews(User specialist) {
        return reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED);
    }

    /**
     * Получение статистики рейтинга
     */
    public ReviewService.RatingStatistics getRatingStatistics(User specialist) {
        List<Review> reviews = reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED);

        if (reviews.isEmpty()) {
            return new ReviewService.RatingStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        long count1 = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 1).count();
        long count2 = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 2).count();
        long count3 = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 3).count();
        long count4 = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 4).count();
        long count5 = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();

        double avgRating = reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        return new ReviewService.RatingStatistics(
                reviews.size(),
                avgRating,
                (double) count5 / reviews.size() * 100,
                (double) count4 / reviews.size() * 100,
                (double) count3 / reviews.size() * 100,
                (double) count2 / reviews.size() * 100,
                (double) count1 / reviews.size() * 100
        );
    }
}