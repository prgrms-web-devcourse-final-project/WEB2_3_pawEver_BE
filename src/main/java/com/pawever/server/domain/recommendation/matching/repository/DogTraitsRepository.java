package com.pawever.server.domain.recommendation.matching.repository;

import com.pawever.server.domain.recommendation.matching.entity.dog.DogTraits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DogTraitsRepository extends JpaRepository<DogTraits, Long> {
}
