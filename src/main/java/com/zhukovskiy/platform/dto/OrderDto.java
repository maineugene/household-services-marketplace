package com.zhukovskiy.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;

    @NotBlank(message = "Описание обязательно")
    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    @Size(max = 500, message = "Адрес не должен превышать 500 символов")
    private String address;

    @Positive(message = "Бюджет должен быть положительным")
    private BigDecimal budget;

    private LocalDateTime deadline;
    private String status;
}