package com.pawever.server.domain.recommendation.matching.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "recommend_pet")
public class RecommendPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendPetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    @Column(nullable = false, length = 50)
    private String breed;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Column(nullable = false, length = 255)
    private String temperament;

    @Column(nullable = false, length = 30)
    private String lifespan;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String precaution;

    private String breedKor;
}
