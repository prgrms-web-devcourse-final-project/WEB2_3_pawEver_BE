package com.pawever.server.domain.recommendation.matching.dto;

import java.util.Map;

public class RecommendationRequest {
    private Map<Integer, Integer> responses; // 질문 ID -> 선택한 옵션 ID

    public Map<Integer, Integer> getResponses() {
        return responses;
    }

    public void setResponses(Map<Integer, Integer> responses) {
        this.responses = responses;
    }
}
