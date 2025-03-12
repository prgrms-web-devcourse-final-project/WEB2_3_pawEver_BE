package com.pawever.server.domain.recommendation.matching.repository;


import com.pawever.server.domain.recommendation.matching.entity.RecommendPet;
import com.pawever.server.domain.recommendation.matching.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface RecommendPetRepository extends JpaRepository<RecommendPet, Long> {
    Optional<RecommendPet> findByBreedAndSpecies(String breed, Species species);

}