package com.zhukovskiy.platform.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order implements BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id")
    private User specialist;

    @Column(nullable = false, length = 2000)
    private String description;

    private String address;

    private BigDecimal budget;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Throws if the order is not in ACTIVE status.
     */
    public void ensureActive() {
        if (this.status != OrderStatus.ACTIVE) {
            throw new IllegalStateException("Заказ уже не активен");
        }
    }
}