package com.pawever.server.domain.recommendation.nearby.dto;

import com.pawever.server.domain.carehub.enums.Sex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRecommendedAnimalResponse {
    private Long id;            // 유기동물 ID
    private String imageUrl;      // 이미지 URL
    private String name;          // 이름
    private String age;           // 나이
    private Sex sex;           //성별
    private String shelterName;   // 보호소 이름
    private BigDecimal distanceKm; // 사용자로부터의 거리 (km)
}