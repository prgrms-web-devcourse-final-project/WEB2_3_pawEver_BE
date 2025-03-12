package com.pawever.server.domain.recommendation.matching.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BreedMatchResult {
    private final String breedName;
    private final double matchScore;
}
