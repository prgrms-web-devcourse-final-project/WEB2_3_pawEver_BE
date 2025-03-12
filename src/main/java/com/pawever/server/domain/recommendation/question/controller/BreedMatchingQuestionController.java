package com.pawever.server.domain.recommendation.question.controller;

import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.recommendation.matching.entity.Species;
import com.pawever.server.domain.recommendation.question.dto.QuestionResponse;
import com.pawever.server.domain.recommendation.question.service.QuestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/recommend-animals")
@RequiredArgsConstructor
@Tag(name = "동물 매칭 질문 API")
public class BreedMatchingQuestionController {

    private final QuestionService questionService;

    @GetMapping("/dogs/questions/{questionId}")
    @Operation(summary = "개 매칭 질문 API")
    public ResponseEntity<ApiResponse> getDogQuestion(@PathVariable Long questionId) {
        QuestionResponse questionResponse =  questionService.getQuestion(Species.DOG, questionId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS, questionResponse));
    }

    @GetMapping("/cats/questions/{questionId}")
    @Operation(summary = "고양이 매칭 질문 API")
    public ResponseEntity<ApiResponse> getCatQuestion(@PathVariable Long questionId) {
        QuestionResponse questionResponse =  questionService.getQuestion(Species.DOG, questionId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS, questionResponse));
    }
}
