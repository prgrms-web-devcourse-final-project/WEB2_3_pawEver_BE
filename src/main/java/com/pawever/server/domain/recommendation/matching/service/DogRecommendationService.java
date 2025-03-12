package com.pawever.server.domain.recommendation.matching.service;

import com.pawever.server.domain.recommendation.matching.dto.BreedMatchResult;
import com.pawever.server.domain.recommendation.matching.dto.RecommendationResponse;
import com.pawever.server.domain.recommendation.matching.dto.TraitImpact;
import com.pawever.server.domain.recommendation.matching.dto.UserTraitPreference;
import com.pawever.server.domain.recommendation.matching.entity.dog.DogTraitType;
import com.pawever.server.domain.recommendation.matching.entity.dog.DogTraits;
import com.pawever.server.domain.recommendation.matching.entity.RecommendPet;
import com.pawever.server.domain.recommendation.matching.entity.Species;
import com.pawever.server.domain.recommendation.matching.repository.DogTraitsRepository;
import com.pawever.server.domain.recommendation.matching.repository.RecommendPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DogRecommendationService {

    private final RecommendPetRepository recommendPetRepository;
    private final DogTraitsRepository dogTraitsRepository;
    private final DogQuestionTraitMappingService dogQuestionTraitMappingService;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendDogs(Map<Integer, Integer> userResponses) {
        //응답을 기준으로 특성 선호도 계산
        Map<String, UserTraitPreference> userTraitPreferences = calculateUserTraitPreferences(userResponses);

        //견종 특성 데이터 조회
        List<DogTraits> allDogTraits = dogTraitsRepository.findAll();

        // 각 견종별 매칭 점수 계산
        List<BreedMatchResult> breedMatchResults = calculateBreedMatchScores(allDogTraits, userTraitPreferences);

        // 매칭 점수로 정렬
        breedMatchResults.sort(Comparator.comparing(BreedMatchResult::getMatchScore).reversed());

        // 상위 1개 견종 선택
        List<BreedMatchResult> topBreeds = breedMatchResults.stream()
                .limit(1)
                .collect(Collectors.toList());

        return createRecommendationResponses(topBreeds);
    }

    //답변에 따라 점수 계산
    private Map<String, UserTraitPreference> calculateUserTraitPreferences(Map<Integer, Integer> userResponses) {

        Map<String, Double> traitScores = new HashMap<>();  //특성별 점수
        Map<String, Double> traitWeights = new HashMap<>();  //특성별 가중치
        Map<String, Boolean> traitTolerances = new HashMap<>();  //무시 여부

        for (Map.Entry<Integer, Integer> response : userResponses.entrySet()) {
            int questionId = response.getKey();
            int optionId = response.getValue();

            // 질문-옵션에 해당하는 특성 영향 정보 전체 조회
            List<TraitImpact> traitImpacts = dogQuestionTraitMappingService.getTraitImpacts(questionId, optionId);

            for (TraitImpact impact : traitImpacts) {
                String traitName = impact.getTraitName();
                double score = impact.getScore();
                double weight = impact.getWeight();
                boolean tolerance = impact.isTolerance();
                boolean reverse = impact.isReverse();

                // 역가중치 처리 (낮은 점수 선호)
                if (reverse) {
                    score = 6 - score; // 5점 척도에서 점수 반전
                }

                traitScores.put(traitName, traitScores.getOrDefault(traitName, 0.0) + score * weight);
                traitWeights.put(traitName, traitWeights.getOrDefault(traitName, 0.0) + weight);

                if (tolerance) {
                    traitTolerances.put(traitName, true);
                }
            }
        }

        // 최종 사용자 특성 선호도 계산
        Map<String, UserTraitPreference> userTraitPreferences = new HashMap<>();
        for (String trait : traitScores.keySet()) {
            double totalWeight = traitWeights.getOrDefault(trait, 0.0);
            if (totalWeight > 0) {
                double finalScore = traitScores.get(trait) / totalWeight;
                boolean tolerance = traitTolerances.getOrDefault(trait, false);
                userTraitPreferences.put(trait, new UserTraitPreference(finalScore, tolerance));
            }
        }

        return userTraitPreferences;
    }

    private List<BreedMatchResult> calculateBreedMatchScores(
            List<DogTraits> dogTraits,
            Map<String, UserTraitPreference> userTraitPreferences) {

        List<BreedMatchResult> results = new ArrayList<>();

        for (DogTraits breed : dogTraits) {
            double totalScore = 0;
            double totalWeight = 0;
            Map<String, Double> traitMatchScores = new HashMap<>();

            // 각 특성별 매칭 점수 계산
            for (Map.Entry<String, UserTraitPreference> entry : userTraitPreferences.entrySet()) {
                String traitName = entry.getKey();
                UserTraitPreference preference = entry.getValue();

                // 견종의 해당 특성 값 조회
                Integer breedTraitValue = getBreedTraitValue(breed, traitName);

                if (breedTraitValue != null) {
                    double matchScore;

                    // 상관없음(tolerance) 처리
                    if (preference.isTolerance()) {
                        matchScore = 1.0; // 최대 매칭 점수
                    } else {
                        // 일반적인 매칭 점수 계산 (0~1 사이 값)
                        double difference = Math.abs(preference.getScore() - breedTraitValue);
                        matchScore = 1.0 - (difference / 4.0);
                    }

                    totalScore += matchScore;
                    totalWeight += 1.0;
                    traitMatchScores.put(traitName, matchScore);
                }
            }

            // 최종 매칭 점수 계산
            double finalMatchScore = totalWeight > 0 ? totalScore / totalWeight : 0;

            // 결과 추가
            results.add(new BreedMatchResult(breed.getBreed(), finalMatchScore));
        }

        return results;
    }

    private Integer getBreedTraitValue(DogTraits breed, String traitName) {
        String formattedName = traitName
                .replaceAll("([a-z])([A-Z])", "$1_$2") // 카멜케이스 → 언더스코어 변환
                .toUpperCase();

        return DogTraitType.valueOf(formattedName).getValue(breed);
    }

    private List<RecommendationResponse> createRecommendationResponses(List<BreedMatchResult> topBreeds) {
        List<RecommendationResponse> responses = new ArrayList<>();

        for (BreedMatchResult result : topBreeds) {
            // 견종 이름으로 RecommendPet 정보 조회
            RecommendPet pet = recommendPetRepository.findByBreedAndSpecies(
                            result.getBreedName(), Species.DOG)
                    .orElse(null);

            if (pet != null) {
                RecommendationResponse response = new RecommendationResponse();
                response.setRecommendPetId(pet.getRecommendPetId());
                response.setBreed(pet.getBreed());
                response.setBreedKor(pet.getBreedKor());
                response.setImageUrl(pet.getImageUrl());
                response.setTemperament(pet.getTemperament());
                response.setLifespan(pet.getLifespan());
                response.setPrecaution(pet.getPrecaution());

                responses.add(response);
            }
        }

        return responses;
    }
}