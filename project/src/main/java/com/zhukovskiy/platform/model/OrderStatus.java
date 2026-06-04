package com.zhukovskiy.platform.model;

public enum OrderStatus {
    ACTIVE,      // активный, ищет исполнителя
    IN_PROGRESS, // в работе
    COMPLETED,   // выполнен
    CANCELLED    // отменен
}