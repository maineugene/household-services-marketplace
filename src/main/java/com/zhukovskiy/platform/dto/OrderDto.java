package com.zhukovskiy.platform.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private String description;
    private String address;
    private BigDecimal budget;
    private LocalDateTime deadline;
    private String status;
}