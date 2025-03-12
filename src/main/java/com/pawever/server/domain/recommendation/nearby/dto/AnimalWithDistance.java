package com.pawever.server.domain.recommendation.nearby.dto;

import com.pawever.server.domain.carehub.entity.AbandonedPet;
import com.pawever.server.domain.carehub.entity.Shelter;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnimalWithDistance {
    private final AbandonedPet pet;
    private final Shelter shelter;
    private final double distance; // km 단위
}
