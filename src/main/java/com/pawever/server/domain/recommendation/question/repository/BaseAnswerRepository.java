package com.pawever.server.domain.recommendation.question.repository;

import com.pawever.server.domain.recommendation.question.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface BaseAnswerRepository  <T extends Answer> extends JpaRepository<T, Long> {
    List<T> findByQuestionId(Long questionId);
}
