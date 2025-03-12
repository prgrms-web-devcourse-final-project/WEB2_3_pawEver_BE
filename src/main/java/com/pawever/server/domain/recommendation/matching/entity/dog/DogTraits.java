package com.pawever.server.domain.recommendation.matching.entity.dog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "dog_traits")
public class DogTraits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer breedId;

    @Column(nullable = false, length = 50)
    private String breed;

    private Integer expressionOfAffection;
    private Integer affectionateWithFamily;
    private Integer childFriendly;
    private Integer dogFriendly;
    private Integer strangerFriendly;
    private Integer shedding;
    private Integer grooming;
    private Integer droolingLevel;
    private Integer healthIssues;
    private Integer trainability;
    private Integer playfulness;
    private Integer energyLevel;
    private Integer exerciseNeeds;
    private Integer apartmentFriendly;
    private Integer catFriendly;
    private Integer barking;
    private Integer socialNeeds;
    private Integer watchdogAbility;
}
