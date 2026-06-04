package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.Review;
import com.zhukovskiy.platform.model.ReviewStatus;
import com.zhukovskiy.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findBySpecialistAndStatus(User specialist, ReviewStatus status);

    boolean existsByCustomerAndSpecialistAndStatus(User customer, User specialist, ReviewStatus status);

    List<Review> findByStatus(ReviewStatus status);

    long countBySpecialistAndStatus(User specialist, ReviewStatus status);
}