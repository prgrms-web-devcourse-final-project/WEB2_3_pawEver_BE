package com.pawever.server.domain.recommendation.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class TraitImpact {
    private final String traitName;
    private final double score;
    private final double weight;
    private final boolean tolerance;
    private final boolean reverse;

}