package com.pawever.server.domain.recommendation.matching.repository;

import com.pawever.server.domain.recommendation.matching.entity.dog.QuestionTrait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionTraitRepository extends JpaRepository<QuestionTrait, Long> {
    List<QuestionTrait> findByQuestionIdAndOptionId(int questionId, int optionId);
}
