package com.pawever.server.domain.recommendation.matching.entity;

public interface PetTraits {
    String getBreed();
    Integer getTraitValue(String traitName);
}
