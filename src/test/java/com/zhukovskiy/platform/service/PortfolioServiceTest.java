package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.PortfolioItemDto;
import com.zhukovskiy.platform.model.PortfolioItem;
import com.zhukovskiy.platform.model.SpecialistProfile;
import com.zhukovskiy.platform.repository.PortfolioItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private SpecialistProfile profile;

    @BeforeEach
    void setUp() {
        profile = SpecialistProfile.builder().id(1L).build();
    }

    @Test
    void getPortfolioBySpecialist_shouldDelegateToRepository() {
        List<PortfolioItem> expected = List.of(PortfolioItem.builder().id(1L).build());
        when(portfolioItemRepository.findBySpecialistProfileOrderByCreatedAtDesc(profile))
                .thenReturn(expected);

        assertThat(portfolioService.getPortfolioBySpecialist(profile)).isEqualTo(expected);
    }

    @Test
    void getPortfolioCount_shouldReturnCount() {
        when(portfolioItemRepository.countBySpecialistProfile(profile)).thenReturn(5);

        assertThat(portfolioService.getPortfolioCount(profile)).isEqualTo(5);
    }

    @Test
    void addPortfolioItem_shouldThrowWhenLimitReached() {
        when(portfolioItemRepository.countBySpecialistProfile(profile)).thenReturn(20);

        PortfolioItemDto dto = PortfolioItemDto.builder().title("Work").build();
        MultipartFile mockFile = mock(MultipartFile.class);

        assertThatThrownBy(() -> portfolioService.addPortfolioItem(profile, dto, mockFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("лимит");
    }

    @Test
    void addPortfolioItem_shouldSaveWithImage() throws IOException {
        when(portfolioItemRepository.countBySpecialistProfile(profile)).thenReturn(5);

        PortfolioItemDto dto = PortfolioItemDto.builder()
                .title("Kitchen renovation")
                .description("Beautiful kitchen")
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        when(portfolioItemRepository.save(any(PortfolioItem.class))).thenAnswer(inv -> {
            PortfolioItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
        });

        PortfolioItem result = portfolioService.addPortfolioItem(profile, dto, mockFile);

        assertThat(result.getTitle()).isEqualTo("Kitchen renovation");
        assertThat(result.getDescription()).isEqualTo("Beautiful kitchen");
        assertThat(result.getImageUrl()).startsWith("/uploads/portfolio/");
        assertThat(result.getImageUrl()).endsWith(".jpg");
        assertThat(result.getSpecialistProfile()).isEqualTo(profile);
    }

    @Test
    void deletePortfolioItem_shouldDeleteWhenOwner() {
        PortfolioItem item = PortfolioItem.builder()
                .id(1L)
                .specialistProfile(profile)
                .imageUrl("/uploads/portfolio/test.jpg")
                .build();

        when(portfolioItemRepository.findById(1L)).thenReturn(Optional.of(item));

        portfolioService.deletePortfolioItem(profile, 1L);

        verify(portfolioItemRepository).delete(item);
    }

    @Test
    void deletePortfolioItem_shouldThrowWhenNotOwner() {
        SpecialistProfile otherProfile = SpecialistProfile.builder().id(2L).build();
        PortfolioItem item = PortfolioItem.builder()
                .id(1L)
                .specialistProfile(otherProfile)
                .imageUrl("/uploads/portfolio/test.jpg")
                .build();

        when(portfolioItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> portfolioService.deletePortfolioItem(profile, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Нет прав");
    }

    @Test
    void deletePortfolioItem_shouldThrowWhenNotFound() {
        when(portfolioItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.deletePortfolioItem(profile, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдена");
    }
}
