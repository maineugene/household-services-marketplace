package com.zhukovskiy.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistProfileDto {
    private Long id;
    private String description;
    private Integer experienceYears;
    private String education;
    private List<String> categories;
    private BigDecimal hourlyRate;
    private BigDecimal fixedPrice;
    private String serviceArea;
    private Double averageRating;
    private List<PortfolioItemDto> portfolio;
}