package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.ReviewDto;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.ReviewRepository;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final SpecialistProfileService specialistProfileService;
    private final SpecialistProfileRepository specialistProfileRepository;

    /**
     * Создание отзыва (только после завершенного заказа)
     */
    @Transactional
    public Review createReview(Order order, User customer, ReviewDto reviewDto) {
        // Проверяем, что заказ выполнен
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Отзыв можно оставить только после выполнения заказа");
        }

        // Проверяем, что отзыв оставляет заказчик
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Только заказчик может оставить отзыв");
        }

        // Проверяем, что заказчик еще не оставлял отзыв этому специалисту
        boolean alreadyReviewed = reviewRepository.existsByCustomerAndSpecialistAndStatus(
                customer, order.getSpecialist(), ReviewStatus.APPROVED);

        if (alreadyReviewed) {
            throw new RuntimeException("Вы уже оставляли отзыв этому специалисту");
        }

        // Получаем профиль специалиста
        SpecialistProfile specialistProfile = specialistProfileRepository.findByUser(order.getSpecialist())
                .orElseThrow(() -> new RuntimeException("Профиль специалиста не найден"));

        Review review = Review.builder()
                .order(order)
                .customer(customer)
                .specialist(order.getSpecialist())
                .specialistProfile(specialistProfile) // Устанавливаем профиль
                .rating(reviewDto.getRating())
                .comment(reviewDto.getComment())
                .status(ReviewStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return reviewRepository.save(review);
    }

    /**
     * Получение отзыва по ID
     */
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден"));
    }

    /**
     * Ответ специалиста на отзыв
     */
    @Transactional
    public Review respondToReview(Long parentReviewId, User specialist, String responseComment) {
        Review parentReview = reviewRepository.findById(parentReviewId)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден"));

        // Проверяем, что отвечает специалист, которому оставлен отзыв
        if (!parentReview.getSpecialist().getId().equals(specialist.getId())) {
            throw new RuntimeException("Только специалист может ответить на отзыв о себе");
        }

        Review response = Review.builder()
                .order(parentReview.getOrder())
                .customer(parentReview.getCustomer())
                .specialist(specialist)
                .parentReview(parentReview)
                .rating(null)
                .comment(responseComment)
                .status(ReviewStatus.APPROVED) // Ответы публикуются сразу
                .createdAt(LocalDateTime.now())
                .build();

        return reviewRepository.save(response);
    }

    /**
     * Модерация отзыва (для модератора/админа)
     */
    @Transactional
    public Review moderateReview(Long reviewId, ReviewStatus status, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден"));

        review.setStatus(status);
        review.setModerationReason(reason);
        review.setUpdatedAt(LocalDateTime.now());

        Review moderatedReview = reviewRepository.save(review);

        // Если отзыв одобрен, пересчитываем рейтинг специалиста
        if (status == ReviewStatus.APPROVED && review.getRating() != null) {
            specialistProfileService.updateRating(review.getSpecialist());
        }

        return moderatedReview;
    }

    /**
     * Получение всех отзывов специалиста
     */
    public List<Review> getReviewsBySpecialist(User specialist) {
        return reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED);
    }

    /**
     * Получение отзывов на модерации
     */
    public List<Review> getPendingReviews() {
        return reviewRepository.findByStatus(ReviewStatus.PENDING);
    }

    /**
     * Расчет статистики рейтингов
     */
    public RatingStatistics getRatingStatistics(User specialist) {
        List<Review> reviews = reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED);

        if (reviews.isEmpty()) {
            return new RatingStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
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

        return new RatingStatistics(
                reviews.size(),
                avgRating,
                (double) count5 / reviews.size() * 100,
                (double) count4 / reviews.size() * 100,
                (double) count3 / reviews.size() * 100,
                (double) count2 / reviews.size() * 100,
                (double) count1 / reviews.size() * 100
        );
    }

    // Вспомогательный record для статистики
    public record RatingStatistics(int totalReviews, double averageRating,
                                   double percent5, double percent4,
                                   double percent3, double percent2, double percent1) {}
}