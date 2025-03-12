package com.pawever.server.domain.recommendation.question.dto;

import com.pawever.server.domain.recommendation.question.entity.Answer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class QuestionResponse {
    private Long questionId;
    private String questionText;
    private List<? extends Answer> answers;
}

