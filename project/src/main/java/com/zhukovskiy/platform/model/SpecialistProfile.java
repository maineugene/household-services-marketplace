package com.zhukovskiy.platform.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "specialist_profiles")
public class SpecialistProfile implements BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 1000)
    private String description;

    @Column(name = "experience_years")
    private Integer experienceYears;

    private String education;

    @ElementCollection
    @CollectionTable(name = "specialist_categories",
            joinColumns = @JoinColumn(name = "specialist_id"))
    @Column(name = "category")
    @Builder.Default
    private List<String> categories = new ArrayList<>();

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "fixed_price")
    private BigDecimal fixedPrice;

    @Column(name = "service_area")
    private String serviceArea;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "moderation_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @OneToMany(mappedBy = "specialistProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortfolioItem> portfolio = new ArrayList<>();

    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @OneToMany(mappedBy = "specialistProfile", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    // Вспомогательные методы для управления портфолио
    public void addPortfolioItem(PortfolioItem item) {
        portfolio.add(item);
        item.setSpecialistProfile(this);
    }

    public void removePortfolioItem(PortfolioItem item) {
        portfolio.remove(item);
        item.setSpecialistProfile(null);
    }
}