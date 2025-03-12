package com.pawever.server.domain.recommendation.nearby.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRecommendedAnimalsRequest {
    private String recommendedBreed; // 추천된 견종 이름
}