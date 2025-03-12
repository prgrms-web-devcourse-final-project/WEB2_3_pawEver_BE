package com.pawever.server.domain.recommendation.matching.repository;

import com.pawever.server.domain.recommendation.matching.entity.cat.CatTraits;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatTraitsRepository extends JpaRepository<CatTraits, Long> {
}
