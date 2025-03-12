package com.pawever.server.domain.recommendation.nearby.controller;

import com.pawever.server.common.response.ApiResponse;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.recommendation.nearby.dto.NearbyRecommendedAnimalResponse;
import com.pawever.server.domain.recommendation.nearby.dto.NearbyRecommendedAnimalsRequest;
import com.pawever.server.domain.recommendation.nearby.service.NearbyRecommendedAnimalsService;
import com.pawever.server.domain.user.jwt.JwtUtil;
import com.pawever.server.domain.user.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommend-animals")
@RequiredArgsConstructor
@Tag(name = "가까운 추천 품종 4마리 조회 API")
public class NearbyPetRecommendationController {

    private final NearbyRecommendedAnimalsService nearbyRecommendedAnimalsService;
    private final AccessTokenService accessTokenService;
    private final JwtUtil jwtUtil;


    @PostMapping("/nearby")
    @Operation(summary = "매칭된 품종 중에서 가까운 4마리 추천 API")
    public ResponseEntity<ApiResponse> getNearbyRecommendedAnimals(
             @RequestBody NearbyRecommendedAnimalsRequest request,
            HttpServletRequest httpServletRequest) {

        String accessToken = accessTokenService.getRequestAccessToken(httpServletRequest);
        Long userId =  jwtUtil.getUserId(accessToken);

        List<NearbyRecommendedAnimalResponse> animals =
                nearbyRecommendedAnimalsService.findNearbyRecommendedAnimals(request, userId);

        return ResponseEntity.ok(ApiResponse.success(ResponseCodeEnum.SUCCESS, animals));
    }


}