package com.pawever.server.domain.recommendation.nearby.service;

import com.pawever.server.common.exception.CustomException;
import com.pawever.server.common.response.ResponseCodeEnum;
import com.pawever.server.domain.carehub.entity.AbandonedPet;
import com.pawever.server.domain.carehub.entity.Shelter;
import com.pawever.server.domain.carehub.repository.AbandonedPetRepository;
import com.pawever.server.domain.carehub.repository.ShelterRepository;
import com.pawever.server.domain.recommendation.nearby.dto.AnimalWithDistance;
import com.pawever.server.domain.recommendation.nearby.dto.NearbyRecommendedAnimalResponse;
import com.pawever.server.domain.recommendation.nearby.dto.NearbyRecommendedAnimalsRequest;
import com.pawever.server.domain.user.entity.jpa.User;
import com.pawever.server.domain.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.pawever.server.common.response.ResponseCodeEnum.LOCATION_NOT_PROVIDED;

@Service
@RequiredArgsConstructor
public class NearbyRecommendedAnimalsService {

    private final UserRepository userRepository;

    private static final int EARTH_RADIUS_KM = 6371; // 지구 반지름 (km)
    private static final int MAX_ANIMALS_TO_RETURN = 4;

    private final AbandonedPetRepository abandonedPetRepository;
    private final ShelterRepository shelterRepository;

    @Transactional(readOnly = true)
    public List<NearbyRecommendedAnimalResponse> findNearbyRecommendedAnimals(
            NearbyRecommendedAnimalsRequest request, Long userId) {

        User user =  userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ResponseCodeEnum.USER_NOT_FOUND));

        if (user.getLatitude()== null && user.getLongitude()== null) {
            throw new CustomException(LOCATION_NOT_PROVIDED);
        }

        // 1. 추천 견종 이름으로 부분 일치 검색
        List<AbandonedPet> matchingPets = abandonedPetRepository.findByBreedContaining(request.getRecommendedBreed());

        if (matchingPets.isEmpty()) {
            return List.of(); // 일치하는 항목이 없는 경우 빈 리스트 반환
        }

        // 2. 모든 관련 보호소 ID 추출
        List<Long> shelterProviderIds = matchingPets.stream()
                .map(AbandonedPet::getProviderShelterId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 관련 보호소 정보 조회
        List<Shelter> shelters = shelterRepository.findByProviderShelterIdIn(shelterProviderIds);

        // 보호소 ID로 보호소 정보에 쉽게 접근하기 위한 맵 생성
        Map<Long, Shelter> shelterMap = shelters.stream()
                .collect(Collectors.toMap(Shelter::getProviderShelterId, shelter -> shelter));

        // 4. 각 유기동물 정보에 거리 계산 추가
        List<AnimalWithDistance> animalsWithDistance = new ArrayList<>();

        for (AbandonedPet pet : matchingPets) {
            Shelter shelter = shelterMap.get(pet.getProviderShelterId());

            // 보호소 정보가 없거나 위도/경도가 없는 경우 건너뜁니다
            if (shelter == null || shelter.getLatitude() == null || shelter.getLongitude() == null) {
                continue;
            }

            // 거리 계산
            double distance = calculateDistance(
                    user.getLatitude().doubleValue(), user.getLongitude().doubleValue(),
                    shelter.getLatitude().doubleValue(), shelter.getLongitude().doubleValue()
            );

            animalsWithDistance.add(new AnimalWithDistance(pet, shelter, distance));
        }

        // 5. 거리순으로 정렬하여 상위 4마리 반환
        return animalsWithDistance.stream()
                .sorted(Comparator.comparingDouble(AnimalWithDistance::getDistance))
                .limit(MAX_ANIMALS_TO_RETURN)
                .map(animalWithDistance -> NearbyRecommendedAnimalResponse.builder()
                        .id(animalWithDistance.getPet().getId()) // 유기동물 ID 추가
                        .imageUrl(animalWithDistance.getPet().getImageUrl())
                        .name(animalWithDistance.getPet().getName())
                        .age(animalWithDistance.getPet().getAge())
                        .sex(animalWithDistance.getPet().getSex())
                        .shelterName(animalWithDistance.getShelter().getName())
                        .distanceKm(BigDecimal.valueOf(animalWithDistance.getDistance())
                                .setScale(1, RoundingMode.HALF_UP))
                        .build()
                )
                .collect(Collectors.toList());
    }

    // Haversine 공식을 사용한 두 지점 간의 거리 계산 (km 단위)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 라디안으로 변환
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        // Haversine 공식
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 거리 계산 (km)
        return EARTH_RADIUS_KM * c;
    }


}