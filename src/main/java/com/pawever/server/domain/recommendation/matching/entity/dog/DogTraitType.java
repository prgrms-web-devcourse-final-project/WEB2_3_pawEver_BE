package com.pawever.server.domain.recommendation.matching.entity.dog;

import java.util.function.Function;

// DogTraits 엔티티에서 특정 특성 값을 가져오는 역할
public enum DogTraitType {
    EXPRESSION_OF_AFFECTION(DogTraits::getExpressionOfAffection),
    AFFECTIONATE_WITH_FAMILY(DogTraits::getAffectionateWithFamily),
    CHILD_FRIENDLY(DogTraits::getChildFriendly),
    DOG_FRIENDLY(DogTraits::getDogFriendly),
    STRANGER_FRIENDLY(DogTraits::getStrangerFriendly),
    SHEDDING(DogTraits::getShedding),
    GROOMING(DogTraits::getGrooming),
    DROOLING_LEVEL(DogTraits::getDroolingLevel),
    HEALTH_ISSUES(DogTraits::getHealthIssues),
    TRAINABILITY(DogTraits::getTrainability),
    PLAYFULNESS(DogTraits::getPlayfulness),
    ENERGY_LEVEL(DogTraits::getEnergyLevel),
    EXERCISE_NEEDS(DogTraits::getExerciseNeeds),
    APARTMENT_FRIENDLY(DogTraits::getApartmentFriendly),
    CAT_FRIENDLY(DogTraits::getCatFriendly),
    BARKING(DogTraits::getBarking),
    SOCIAL_NEEDS(DogTraits::getSocialNeeds),
    WATCHDOG_ABILITY(DogTraits::getWatchdogAbility);

    private final Function<DogTraits, Integer> getter;

    DogTraitType(Function<DogTraits, Integer> getter) {
        this.getter = getter;
    }

    public Integer getValue(DogTraits dogTraits) {
        return getter.apply(dogTraits);
    }

}

