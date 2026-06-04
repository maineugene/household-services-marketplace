package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.Review;
import com.zhukovskiy.platform.model.ReviewStatus;
import com.zhukovskiy.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findBySpecialistAndStatus(User specialist, ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.specialist = ?1 AND r.status = 'APPROVED'")
    Double calculateAverageRatingForSpecialist(User specialist);

    boolean existsByCustomerAndSpecialistAndStatus(User customer, User specialist, ReviewStatus status);

    List<Review> findByStatus(ReviewStatus status);

    long countBySpecialistAndStatus(User specialist, ReviewStatus status);
}