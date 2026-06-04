package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.model.ModerationStatus;
import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.SpecialistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SpecialistProfileRepository specialistProfileRepository;

    @InjectMocks
    private SearchService searchService;

    private List<SpecialistProfile> allSpecialists;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L).email("a@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("p").role(Role.SPECIALIST)
                .dob(LocalDate.of(1990, 1, 1)).build();

        user2 = User.builder()
                .id(2L).email("b@test.com").firstName("Petr").lastName("Petrov")
                .country("Russia").password("p").role(Role.SPECIALIST)
                .dob(LocalDate.of(1985, 5, 15)).build();

        user3 = User.builder()
                .id(3L).email("c@test.com").firstName("Anna").lastName("Sidorova")
                .country("Russia").password("p").role(Role.SPECIALIST)
                .dob(LocalDate.of(1992, 3, 20)).build();

        SpecialistProfile sp1 = SpecialistProfile.builder()
                .id(1L).user(user1).description("Expert plumber")
                .categories(new ArrayList<>(List.of("Сантехника")))
                .hourlyRate(new BigDecimal("1500")).experienceYears(10)
                .serviceArea("Moscow").averageRating(4.8)
                .moderationStatus(ModerationStatus.APPROVED).isVerified(true)
                .build();

        SpecialistProfile sp2 = SpecialistProfile.builder()
                .id(2L).user(user2).description("Electrician")
                .categories(new ArrayList<>(List.of("Электрика")))
                .hourlyRate(new BigDecimal("2000")).experienceYears(5)
                .serviceArea("Saint Petersburg").averageRating(4.2)
                .moderationStatus(ModerationStatus.APPROVED).isVerified(true)
                .build();

        SpecialistProfile sp3 = SpecialistProfile.builder()
                .id(3L).user(user3).description("Cleaning services")
                .categories(new ArrayList<>(List.of("Уборка")))
                .fixedPrice(new BigDecimal("3000")).experienceYears(3)
                .serviceArea("Moscow").averageRating(4.5)
                .moderationStatus(ModerationStatus.APPROVED).isVerified(true)
                .build();

        allSpecialists = new ArrayList<>(List.of(sp1, sp2, sp3));
    }

    @Test
    void searchSpecialists_noFilters_shouldReturnAllSortedByRating() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, null, null, null, "rating_desc");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAverageRating()).isGreaterThanOrEqualTo(result.get(1).getAverageRating());
    }

    @Test
    void searchSpecialists_filterByCategory() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                "Сантехника", null, null, null, null, null, null, "rating_desc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategories()).contains("Сантехника");
    }

    @Test
    void searchSpecialists_filterByQuery() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, "plumber", null, null, null, null, null, "rating_desc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).containsIgnoringCase("plumber");
    }

    @Test
    void searchSpecialists_filterByQueryMatchesName() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, "anna", null, null, null, null, null, "rating_desc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getFirstName()).isEqualToIgnoringCase("Anna");
    }

    @Test
    void searchSpecialists_filterByMinPrice() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, new BigDecimal("2000"), null, null, null, null, "rating_desc");

        assertThat(result).hasSize(2); // user2 (hourly=2000) and user3 (fixed=3000)
    }

    @Test
    void searchSpecialists_filterByMaxPrice() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, new BigDecimal("1500"), null, null, null, "rating_desc");

        assertThat(result).hasSize(1); // user1 (hourly=1500)
    }

    @Test
    void searchSpecialists_filterByMinRating() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, 5, null, null, "rating_desc");

        assertThat(result).isEmpty();
    }

    @Test
    void searchSpecialists_filterByMinExperience() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, null, 6, null, "rating_desc");

        assertThat(result).hasSize(1); // user1 (10 yrs)
    }

    @Test
    void searchSpecialists_filterByLocation() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, null, null, "Moscow", "rating_desc");

        assertThat(result).hasSize(2); // user1 and user3
    }

    @Test
    void searchSpecialists_sortByPriceAsc() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, null, null, null, "price_asc");

        assertThat(result).hasSize(3);
        // First should have lowest min price
    }

    @Test
    void searchSpecialists_sortByExperienceDesc() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.searchSpecialists(
                null, null, null, null, null, null, null, "experience_desc");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getExperienceYears()).isGreaterThanOrEqualTo(result.get(1).getExperienceYears());
    }

    @Test
    void findSpecialistsNearby_shouldFilterByAddress() {
        when(specialistProfileRepository.findActiveSpecialists()).thenReturn(new ArrayList<>(allSpecialists));

        List<SpecialistProfile> result = searchService.findSpecialistsNearby("Moscow", 10);

        assertThat(result).hasSize(2);
    }

    @Test
    void getAvailableFilters_shouldReturnFilters() {
        SearchService.SearchFilters filters = searchService.getAvailableFilters();

        assertThat(filters.getCategories()).isNotEmpty();
        assertThat(filters.getSortOptions()).isNotEmpty();
        assertThat(filters.getPriceRange()).isNotNull();
        assertThat(filters.getRatingRange()).isNotNull();
        assertThat(filters.getExperienceRange()).isNotNull();
    }
}
