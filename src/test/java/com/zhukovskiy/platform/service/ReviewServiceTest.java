package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.ReviewDto;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.OrderRepository;
import com.zhukovskiy.platform.repository.ReviewRepository;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SpecialistProfileService specialistProfileService;

    @Mock
    private SpecialistProfileRepository specialistProfileRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private User specialist;
    private Order completedOrder;
    private SpecialistProfile specialistProfile;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .country("Russia")
                .password("pass")
                .role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1))
                .build();

        specialist = User.builder()
                .id(2L)
                .email("specialist@test.com")
                .firstName("Petr")
                .lastName("Petrov")
                .country("Russia")
                .password("pass")
                .role(Role.SPECIALIST)
                .dob(LocalDate.of(1985, 5, 15))
                .build();

        specialistProfile = SpecialistProfile.builder()
                .id(1L)
                .user(specialist)
                .averageRating(4.5)
                .build();

        completedOrder = Order.builder()
                .id(1L)
                .customer(customer)
                .specialist(specialist)
                .status(OrderStatus.COMPLETED)
                .build();
    }

    @Test
    void createReview_shouldSaveReview() {
        ReviewDto dto = ReviewDto.builder().rating(5).comment("Excellent!").build();

        when(reviewRepository.existsByCustomerAndSpecialistAndStatus(customer, specialist, ReviewStatus.APPROVED))
                .thenReturn(false);
        when(specialistProfileRepository.findByUser(specialist))
                .thenReturn(Optional.of(specialistProfile));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        Review result = reviewService.createReview(completedOrder, customer, dto);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent!");
        assertThat(result.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getSpecialist()).isEqualTo(specialist);
        assertThat(result.getSpecialistProfile()).isEqualTo(specialistProfile);
    }

    @Test
    void createReview_shouldThrowWhenOrderNotCompleted() {
        Order activeOrder = Order.builder()
                .id(2L)
                .customer(customer)
                .specialist(specialist)
                .status(OrderStatus.ACTIVE)
                .build();

        ReviewDto dto = ReviewDto.builder().rating(5).build();

        assertThatThrownBy(() -> reviewService.createReview(activeOrder, customer, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("выполнения");
    }

    @Test
    void createReview_shouldThrowWhenNotCustomer() {
        ReviewDto dto = ReviewDto.builder().rating(5).build();

        assertThatThrownBy(() -> reviewService.createReview(completedOrder, specialist, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("заказчик");
    }

    @Test
    void createReview_shouldThrowWhenAlreadyReviewed() {
        ReviewDto dto = ReviewDto.builder().rating(5).build();

        when(reviewRepository.existsByCustomerAndSpecialistAndStatus(customer, specialist, ReviewStatus.APPROVED))
                .thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(completedOrder, customer, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("уже оставляли");
    }

    @Test
    void getReviewById_shouldReturnReview() {
        Review review = Review.builder().id(1L).rating(4).build();
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThat(reviewService.getReviewById(1L)).isEqualTo(review);
    }

    @Test
    void getReviewById_shouldThrowWhenNotFound() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void respondToReview_shouldSaveResponse() {
        Review parentReview = Review.builder()
                .id(1L)
                .order(completedOrder)
                .customer(customer)
                .specialist(specialist)
                .rating(5)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(parentReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        Review response = reviewService.respondToReview(1L, specialist, "Thank you!");

        assertThat(response.getComment()).isEqualTo("Thank you!");
        assertThat(response.getParentReview()).isEqualTo(parentReview);
        assertThat(response.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(response.getRating()).isNull();
    }

    @Test
    void respondToReview_shouldThrowWhenNotOwner() {
        Review parentReview = Review.builder()
                .id(1L)
                .specialist(specialist)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(parentReview));

        assertThatThrownBy(() -> reviewService.respondToReview(1L, customer, "Thanks"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("специалист");
    }

    @Test
    void moderateReview_shouldApproveAndUpdateRating() {
        Review review = Review.builder()
                .id(1L)
                .specialist(specialist)
                .rating(5)
                .status(ReviewStatus.PENDING)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.moderateReview(1L, ReviewStatus.APPROVED, "Looks good");

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(result.getModerationReason()).isEqualTo("Looks good");
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(specialistProfileService).updateRating(specialist);
    }

    @Test
    void moderateReview_shouldRejectWithoutUpdatingRating() {
        Review review = Review.builder()
                .id(1L)
                .specialist(specialist)
                .rating(1)
                .status(ReviewStatus.PENDING)
                .build();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.moderateReview(1L, ReviewStatus.REJECTED, "Spam");

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        verify(specialistProfileService, never()).updateRating(any());
    }

    @Test
    void getReviewsBySpecialist_shouldReturnApproved() {
        List<Review> expected = List.of(Review.builder().id(1L).build());
        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(expected);

        assertThat(reviewService.getReviewsBySpecialist(specialist)).isEqualTo(expected);
    }

    @Test
    void getPendingReviews_shouldReturnPending() {
        List<Review> expected = List.of(Review.builder().id(1L).status(ReviewStatus.PENDING).build());
        when(reviewRepository.findByStatus(ReviewStatus.PENDING)).thenReturn(expected);

        assertThat(reviewService.getPendingReviews()).isEqualTo(expected);
    }

    @Test
    void getRatingStatistics_shouldReturnCorrectStats() {
        List<Review> reviews = List.of(
                Review.builder().id(1L).rating(5).build(),
                Review.builder().id(2L).rating(5).build(),
                Review.builder().id(3L).rating(4).build(),
                Review.builder().id(4L).rating(3).build()
        );

        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(reviews);

        ReviewService.RatingStatistics stats = reviewService.getRatingStatistics(specialist);

        assertThat(stats.totalReviews()).isEqualTo(4);
        assertThat(stats.averageRating()).isEqualTo(4.25);
        assertThat(stats.percent5()).isEqualTo(50.0);
        assertThat(stats.percent4()).isEqualTo(25.0);
        assertThat(stats.percent3()).isEqualTo(25.0);
        assertThat(stats.percent2()).isEqualTo(0.0);
        assertThat(stats.percent1()).isEqualTo(0.0);
    }

    @Test
    void getRatingStatistics_shouldReturnZerosWhenEmpty() {
        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        ReviewService.RatingStatistics stats = reviewService.getRatingStatistics(specialist);

        assertThat(stats.totalReviews()).isEqualTo(0);
        assertThat(stats.averageRating()).isEqualTo(0.0);
    }
}
