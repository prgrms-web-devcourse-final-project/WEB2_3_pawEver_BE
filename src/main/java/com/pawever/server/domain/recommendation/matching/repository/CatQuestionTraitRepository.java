package com.pawever.server.domain.recommendation.matching.repository;

import com.pawever.server.domain.recommendation.matching.entity.cat.CatQuestionTrait;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatQuestionTraitRepository extends JpaRepository<CatQuestionTrait, Long> {
    List<CatQuestionTrait> findByQuestionIdAndOptionId(int questionId, int optionId);
}
