package com.pawever.server.domain.recommendation.matching.controller;

import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.recommendation.matching.dto.RecommendationRequest;
import com.pawever.server.domain.recommendation.matching.dto.RecommendationResponse;
import com.pawever.server.domain.recommendation.matching.service.CatRecommendationService;
import com.pawever.server.domain.recommendation.matching.service.DogRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend-animals")
@RequiredArgsConstructor
@Tag(name = "동물 매칭 & 추천 API")
public class BreedMatchingController {

    private final DogRecommendationService dogRecommendationService;
    private final CatRecommendationService catRecommendationService;


    @PostMapping("/dog")
    @Operation(summary = "개 매칭 API")
    public ResponseEntity<ApiResponse> recommendDog( @RequestBody RecommendationRequest request) {
        List<RecommendationResponse> recommendations = dogRecommendationService.recommendDogs(request.getResponses());

        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS, recommendations));
    }

    @PostMapping("/cat")
    @Operation(summary = "고양이 매칭 API")
    public ResponseEntity<ApiResponse> recommendCat( @RequestBody RecommendationRequest request) {
        List<RecommendationResponse> recommendations = catRecommendationService.recommendCats(request.getResponses());

        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS, recommendations));
    }


}