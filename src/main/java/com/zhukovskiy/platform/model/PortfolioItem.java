package com.zhukovskiy.platform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "portfolio_items")
public class PortfolioItem implements BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;  // Это поле должно быть

    private String title;

    @Column(length = 500)
    private String description;

    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}