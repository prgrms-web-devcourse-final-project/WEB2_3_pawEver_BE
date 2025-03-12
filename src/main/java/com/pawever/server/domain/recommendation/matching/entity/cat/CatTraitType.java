package com.pawever.server.domain.recommendation.matching.entity.cat;

import java.util.function.Function;

public enum CatTraitType {
    ADAPTABILITY(CatTraits::getAdaptability),
    CHILD_FRIENDLY(CatTraits::getChildFriendly),
    SHEDDING_LEVEL(CatTraits::getSheddingLevel),
    HEALTH_ISSUES(CatTraits::getHealthIssues),
    AFFECTIONATE(CatTraits::getAffectionate),
    INTELLIGENCE(CatTraits::getIntelligence),
    ENERGY_LEVEL(CatTraits::getEnergyLevel),
    SOCIAL_NEEDS(CatTraits::getSocialNeeds),
    GROOMING(CatTraits::getGrooming),
    STRANGER_FRIENDLY(CatTraits::getStrangerFriendly),
    DOG_FRIENDLY(CatTraits::getDogFriendly);

    private final Function<CatTraits, Integer> getter;

    CatTraitType(Function<CatTraits, Integer> getter) {
        this.getter = getter;
    }

    public Integer getValue(CatTraits catTraits) {
        return getter.apply(catTraits);
    }
}