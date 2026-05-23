package com.zhukovskiy.platform.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
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