package com.zhukovskiy.platform.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BidDto {
    private BigDecimal price;
    private String comment;
}