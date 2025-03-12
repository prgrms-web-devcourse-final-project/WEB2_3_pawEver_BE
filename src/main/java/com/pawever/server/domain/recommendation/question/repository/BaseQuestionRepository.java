package com.pawever.server.domain.recommendation.question.repository;

import com.pawever.server.domain.recommendation.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseQuestionRepository <T extends Question> extends JpaRepository<T, Long> {

}
