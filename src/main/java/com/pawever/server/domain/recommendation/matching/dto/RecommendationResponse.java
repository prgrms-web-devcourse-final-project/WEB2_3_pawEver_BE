package com.pawever.server.domain.recommendation.matching.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationResponse {
    private Long recommendPetId;
    private String breed;
    private String breedKor;
    private String imageUrl;
    private String temperament;
    private String lifespan;
    private String precaution;
}