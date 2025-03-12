package com.pawever.server.domain.recommendation.question.service;

import com.pawever.server.common.exception.CustomException;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.recommendation.matching.entity.Species;
import com.pawever.server.domain.recommendation.question.entity.Answer;
import com.pawever.server.domain.recommendation.question.dto.QuestionResponse;
import com.pawever.server.domain.recommendation.question.entity.Question;
import com.pawever.server.domain.recommendation.question.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class QuestionService {
    private final Map<Species, BaseQuestionRepository<?>> questionRepositoryMap;
    private final Map<Species, BaseAnswerRepository<?>> answerRepositoryMap;

    public QuestionService(DogQuestionRepository dogQuestionRepo, CatQuestionRepository catQuestionRepo, DogAnswerRepository dogAnswerRepo, CatAnswerRepository catAnswerRepo) {
        this.questionRepositoryMap = Map.of(
                Species.DOG, dogQuestionRepo,
                Species.CAT, catQuestionRepo
        );

        this.answerRepositoryMap = Map.of(
                Species.DOG, dogAnswerRepo,
                Species.CAT, catAnswerRepo
        );
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Species species, Long questionId) {
        BaseQuestionRepository<?> questionRepository = questionRepositoryMap.get(species);
        if (questionRepository == null) {
            throw new CustomException(ResponseCodeEnum.QUESTION_NOT_FOUND);
        }

        Question question = (Question) questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ResponseCodeEnum.QUESTION_NOT_FOUND));

        BaseAnswerRepository<?> answerRepository = answerRepositoryMap.get(species);
        if (answerRepository == null) {
            throw new CustomException(ResponseCodeEnum.ANSWER_NOT_FOUND);
        }

        List<? extends Answer> answer =  answerRepository.findByQuestionId(questionId);  //없으면 빈 리스트

        return new QuestionResponse(question.getQuestionId(), question.getQuestionText(), answer);
    }
}
