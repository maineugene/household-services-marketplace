package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.ModerationStatus;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    // Базовые методы
    Optional<SpecialistProfile> findByUser(User user);

    List<SpecialistProfile> findByModerationStatus(ModerationStatus status);

    List<SpecialistProfile> findByIsVerifiedTrueAndModerationStatus(ModerationStatus status);

    // Поиск по категориям с использованием JOIN
    @Query("SELECT DISTINCT s FROM SpecialistProfile s JOIN s.categories c WHERE c IN :categories AND s.isVerified = true AND s.moderationStatus = 'APPROVED'")
    List<SpecialistProfile> findByCategories(@Param("categories") List<String> categories);

    // Поиск по одной категории
    @Query("SELECT DISTINCT s FROM SpecialistProfile s JOIN s.categories c WHERE c = :category AND s.isVerified = true AND s.moderationStatus = 'APPROVED'")
    List<SpecialistProfile> findBySingleCategory(@Param("category") String category);

    // Активные специалисты
    @Query("SELECT s FROM SpecialistProfile s WHERE s.isVerified = true AND s.moderationStatus = 'APPROVED'")
    List<SpecialistProfile> findActiveSpecialists();

    // Расчет среднего рейтинга
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.specialist = :specialist AND r.status = 'APPROVED' AND r.rating IS NOT NULL")
    Double calculateAverageRatingForSpecialist(@Param("specialist") User specialist);
}