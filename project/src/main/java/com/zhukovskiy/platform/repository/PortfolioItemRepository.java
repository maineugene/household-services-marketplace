package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.PortfolioItem;
import com.zhukovskiy.platform.model.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findBySpecialistProfileOrderByCreatedAtDesc(SpecialistProfile specialistProfile);

    // Исправлено: countBySpecialistProfile
    int countBySpecialistProfile(SpecialistProfile specialistProfile);
}