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
    Optional<SpecialistProfile> findByUser(User user);

    List<SpecialistProfile> findByModerationStatus(ModerationStatus status);

    @Query("SELECT s FROM SpecialistProfile s WHERE s.isVerified = true AND s.moderationStatus = 'APPROVED'")
    List<SpecialistProfile> findActiveSpecialists();

    @Query("SELECT s FROM SpecialistProfile s WHERE s.categories IN :categories AND s.isVerified = true")
    List<SpecialistProfile> findByCategories(@Param("categories") List<String> categories);
}