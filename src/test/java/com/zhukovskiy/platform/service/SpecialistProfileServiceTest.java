package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.SpecialistProfileDto;
import com.zhukovskiy.platform.mapper.SpecialistMapper;
import com.zhukovskiy.platform.model.*;
import com.zhukovskiy.platform.repository.ReviewRepository;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import com.zhukovskiy.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialistProfileServiceTest {

    @Mock
    private SpecialistProfileRepository specialistProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpecialistMapper specialistMapper;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private SpecialistProfileService service;

    private User specialist;
    private User customer;
    private SpecialistProfileDto profileDto;

    @BeforeEach
    void setUp() {
        specialist = User.builder()
                .id(1L).email("s@test.com").firstName("Petr").lastName("Petrov")
                .country("Russia").password("p").role(Role.SPECIALIST)
                .dob(LocalDate.of(1985, 5, 15)).build();

        customer = User.builder()
                .id(2L).email("c@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("p").role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1)).build();

        profileDto = SpecialistProfileDto.builder()
                .description("Expert plumber")
                .experienceYears(10)
                .education("Technical University")
                .categories(List.of("Сантехника"))
                .hourlyRate(new BigDecimal("1500"))
                .serviceArea("Moscow")
                .build();
    }

    @Test
    void createOrUpdateProfile_shouldCreateNewProfile() {
        SpecialistProfile newProfile = SpecialistProfile.builder()
                .description("Expert plumber")
                .build();

        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.empty());
        when(specialistMapper.toEntity(profileDto)).thenReturn(newProfile);
        when(specialistProfileRepository.save(any(SpecialistProfile.class)))
                .thenAnswer(inv -> {
                    SpecialistProfile p = inv.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        SpecialistProfile result = service.createOrUpdateProfile(specialist, profileDto);

        assertThat(result.getUser()).isEqualTo(specialist);
        assertThat(result.getModerationStatus()).isEqualTo(ModerationStatus.PENDING);
        assertThat(result.getIsVerified()).isFalse();
        assertThat(result.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void createOrUpdateProfile_shouldUpdateExistingProfile() {
        SpecialistProfile existingProfile = SpecialistProfile.builder()
                .id(1L).user(specialist).description("Old desc")
                .moderationStatus(ModerationStatus.APPROVED).isVerified(true)
                .averageRating(4.5).build();

        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.of(existingProfile));
        when(specialistProfileRepository.save(any(SpecialistProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SpecialistProfile result = service.createOrUpdateProfile(specialist, profileDto);

        assertThat(result.getDescription()).isEqualTo("Expert plumber");
        assertThat(result.getExperienceYears()).isEqualTo(10);
        assertThat(result.getModerationStatus()).isEqualTo(ModerationStatus.PENDING);
        assertThat(result.getIsVerified()).isFalse();
    }

    @Test
    void createOrUpdateProfile_shouldThrowForCustomer() {
        assertThatThrownBy(() -> service.createOrUpdateProfile(customer, profileDto))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getProfileByUser_shouldReturnProfile() {
        SpecialistProfile profile = SpecialistProfile.builder().id(1L).user(specialist).build();
        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.of(profile));

        assertThat(service.getProfileByUser(specialist)).isEqualTo(profile);
    }

    @Test
    void getProfileByUser_shouldThrowWhenNotFound() {
        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileByUser(specialist))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getProfileById_shouldReturnProfile() {
        SpecialistProfile profile = SpecialistProfile.builder().id(1L).build();
        when(specialistProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThat(service.getProfileById(1L)).isEqualTo(profile);
    }

    @Test
    void getProfileById_shouldThrowWhenNotFound() {
        when(specialistProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void moderateProfile_shouldApproveAndVerify() {
        SpecialistProfile profile = SpecialistProfile.builder()
                .id(1L).moderationStatus(ModerationStatus.PENDING).isVerified(false).build();

        when(specialistProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(specialistProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.moderateProfile(1L, ModerationStatus.APPROVED);

        assertThat(profile.getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(profile.getIsVerified()).isTrue();
    }

    @Test
    void moderateProfile_shouldRejectWithoutVerifying() {
        SpecialistProfile profile = SpecialistProfile.builder()
                .id(1L).moderationStatus(ModerationStatus.PENDING).isVerified(false).build();

        when(specialistProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(specialistProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.moderateProfile(1L, ModerationStatus.REJECTED);

        assertThat(profile.getModerationStatus()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(profile.getIsVerified()).isFalse();
    }

    @Test
    void updateRating_shouldCalculateAndSave() {
        SpecialistProfile profile = SpecialistProfile.builder()
                .id(1L).user(specialist).averageRating(0.0).build();

        when(specialistProfileRepository.calculateAverageRatingForSpecialist(specialist)).thenReturn(4.5);
        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.of(profile));
        when(specialistProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateRating(specialist);

        assertThat(profile.getAverageRating()).isEqualTo(4.5);
    }

    @Test
    void updateRating_shouldDefaultToZeroWhenNull() {
        SpecialistProfile profile = SpecialistProfile.builder()
                .id(1L).user(specialist).averageRating(3.0).build();

        when(specialistProfileRepository.calculateAverageRatingForSpecialist(specialist)).thenReturn(null);
        when(specialistProfileRepository.findByUser(specialist)).thenReturn(Optional.of(profile));
        when(specialistProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateRating(specialist);

        assertThat(profile.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void getActiveSpecialists_shouldDelegateToRepository() {
        List<SpecialistProfile> expected = List.of(SpecialistProfile.builder().id(1L).build());
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(expected);

        assertThat(service.getActiveSpecialists()).isEqualTo(expected);
    }

    @Test
    void getProfilesForModeration_shouldReturnPending() {
        List<SpecialistProfile> expected = List.of(SpecialistProfile.builder().id(1L).build());
        when(specialistProfileRepository.findByModerationStatus(ModerationStatus.PENDING)).thenReturn(expected);

        assertThat(service.getProfilesForModeration()).isEqualTo(expected);
    }

    @Test
    void getAllCategories_shouldReturnNonEmptyList() {
        assertThat(service.getAllCategories()).isNotEmpty();
    }

    @Test
    void getApprovedReviews_shouldReturnApprovedOnly() {
        List<Review> expected = List.of(Review.builder().id(1L).status(ReviewStatus.APPROVED).build());
        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(expected);

        assertThat(service.getApprovedReviews(specialist)).isEqualTo(expected);
    }

    @Test
    void getRatingStatistics_shouldComputeCorrectly() {
        List<Review> reviews = List.of(
                Review.builder().id(1L).rating(5).build(),
                Review.builder().id(2L).rating(4).build()
        );

        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(reviews);

        ReviewService.RatingStatistics stats = service.getRatingStatistics(specialist);

        assertThat(stats.totalReviews()).isEqualTo(2);
        assertThat(stats.averageRating()).isEqualTo(4.5);
    }

    @Test
    void getRatingStatistics_shouldReturnZerosWhenEmpty() {
        when(reviewRepository.findBySpecialistAndStatus(specialist, ReviewStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        ReviewService.RatingStatistics stats = service.getRatingStatistics(specialist);

        assertThat(stats.totalReviews()).isEqualTo(0);
        assertThat(stats.averageRating()).isEqualTo(0.0);
    }

    @Test
    void convertToDto_shouldDelegateToMapper() {
        SpecialistProfile profile = SpecialistProfile.builder().id(1L).build();
        SpecialistProfileDto dto = SpecialistProfileDto.builder().description("test").build();
        when(specialistMapper.toDto(profile)).thenReturn(dto);

        assertThat(service.convertToDto(profile)).isEqualTo(dto);
    }
}
