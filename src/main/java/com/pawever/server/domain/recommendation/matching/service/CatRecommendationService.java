package com.pawever.server.domain.recommendation.matching.service;

import com.pawever.server.domain.recommendation.matching.dto.BreedMatchResult;
import com.pawever.server.domain.recommendation.matching.dto.RecommendationResponse;
import com.pawever.server.domain.recommendation.matching.dto.TraitImpact;
import com.pawever.server.domain.recommendation.matching.dto.UserTraitPreference;
import com.pawever.server.domain.recommendation.matching.entity.cat.CatTraitType;
import com.pawever.server.domain.recommendation.matching.entity.cat.CatTraits;
import com.pawever.server.domain.recommendation.matching.entity.RecommendPet;
import com.pawever.server.domain.recommendation.matching.entity.Species;
import com.pawever.server.domain.recommendation.matching.repository.CatTraitsRepository;
import com.pawever.server.domain.recommendation.matching.repository.RecommendPetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatRecommendationService {

    private final RecommendPetRepository recommendPetRepository;
    private final CatTraitsRepository catTraitsRepository;
    private final CatQuestionTraitMappingService catQuestionTraitMappingService;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendCats(Map<Integer, Integer> userResponses) {
        //응답을 기준으로 특성 선호도 계산
        Map<String, UserTraitPreference> userTraitPreferences = calculateUserTraitPreferences(userResponses);

        //고양이 특성 데이터 조회
        List<CatTraits> allCatTraits = catTraitsRepository.findAll();

        // 각 고양이 품종별 매칭 점수 계산
        List<BreedMatchResult> breedMatchResults = calculateBreedMatchScores(allCatTraits, userTraitPreferences);

        // 매칭 점수로 정렬
        breedMatchResults.sort(Comparator.comparing(BreedMatchResult::getMatchScore).reversed());

        // 상위 1개 품종 선택
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
            List<TraitImpact> traitImpacts = catQuestionTraitMappingService.getTraitImpacts(questionId, optionId);

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

    // calculateBreedMatchScores 메소드에 디버깅 로그 추가
    private List<BreedMatchResult> calculateBreedMatchScores(
            List<CatTraits> catTraits,
            Map<String, UserTraitPreference> userTraitPreferences) {

        List<BreedMatchResult> results = new ArrayList<>();
        log.debug("User trait preferences: {}", userTraitPreferences);
        log.debug("Number of cat breeds to evaluate: {}", catTraits.size());

        for (CatTraits breed : catTraits) {
            double totalScore = 0;
            double totalWeight = 0;
            Map<String, Double> traitMatchScores = new HashMap<>();
            log.debug("Evaluating breed: {}", breed.getBreed());

            // 각 특성별 매칭 점수 계산
            for (Map.Entry<String, UserTraitPreference> entry : userTraitPreferences.entrySet()) {
                String traitName = entry.getKey();
                UserTraitPreference preference = entry.getValue();

                // 품종의 해당 특성 값 조회
                Integer breedTraitValue = getBreedTraitValue(breed, traitName);

                log.debug("  Trait: {}, User preference: {}, Breed value: {}",
                        traitName, preference.getScore(), breedTraitValue);

                if (breedTraitValue != null) {
                    double matchScore;

                    // 상관없음(tolerance) 처리
                    if (preference.isTolerance()) {
                        matchScore = 1.0; // 최대 매칭 점수
                        log.debug("  -> Tolerance applied for trait {}", traitName);
                    } else {
                        // 일반적인 매칭 점수 계산 (0~1 사이 값)
                        double difference = Math.abs(preference.getScore() - breedTraitValue);
                        matchScore = 1.0 - (difference / 4.0);
                    }

                    log.debug("  -> Match score for trait {}: {}", traitName, matchScore);
                    totalScore += matchScore;
                    totalWeight += 1.0;
                    traitMatchScores.put(traitName, matchScore);
                } else {
                    log.debug("  -> Trait value is null, skipping");
                }
            }

            // 최종 매칭 점수 계산
            double finalMatchScore = totalWeight > 0 ? totalScore / totalWeight : 0;
            log.debug("Final match score for {}: {}", breed.getBreed(), finalMatchScore);

            // 결과 추가
            results.add(new BreedMatchResult(breed.getBreed(), finalMatchScore));
        }

        // 결과 정렬 전 로깅
        log.debug("Unsorted match results: {}", results.stream()
                .map(r -> r.getBreedName() + ": " + r.getMatchScore())
                .collect(Collectors.joining(", ")));

        return results;
    }

    private Integer getBreedTraitValue(CatTraits breed, String traitName) {
        String formattedName = traitName
                .replaceAll("([a-z])([A-Z])", "$1_$2") // 카멜케이스 → 언더스코어 변환
                .toUpperCase();

        try {
            return CatTraitType.valueOf(formattedName).getValue(breed);
        } catch (IllegalArgumentException e) {
            // 해당 특성이 CatTraitType에 없는 경우
            return null;
        }
    }

    private List<RecommendationResponse> createRecommendationResponses(List<BreedMatchResult> topBreeds) {
        List<RecommendationResponse> responses = new ArrayList<>();

        for (BreedMatchResult result : topBreeds) {
            // 품종 이름으로 RecommendPet 정보 조회
            RecommendPet pet = recommendPetRepository.findByBreedAndSpecies(
                            result.getBreedName(), Species.CAT)
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
