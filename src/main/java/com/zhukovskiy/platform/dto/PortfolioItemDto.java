package com.zhukovskiy.platform.dto;

import lombok.Data;

@Data
public class PortfolioItemDto {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
}