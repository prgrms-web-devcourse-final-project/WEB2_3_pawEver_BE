package com.pawever.server.domain.recommendation.matching.service;

import com.pawever.server.domain.recommendation.matching.dto.TraitImpact;
import com.pawever.server.domain.recommendation.matching.entity.cat.CatQuestionTrait;
import com.pawever.server.domain.recommendation.matching.repository.CatQuestionTraitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatQuestionTraitMappingService {

    private final CatQuestionTraitRepository catQuestionTraitRepository;

    public List<TraitImpact> getTraitImpacts(int questionId, int optionId) {
        List<CatQuestionTrait> traits = catQuestionTraitRepository.findByQuestionIdAndOptionId(questionId, optionId);

        return traits.stream()
                .map(trait -> new TraitImpact(
                        trait.getTraitName(),
                        trait.getScore(),
                        trait.getWeight(),
                        trait.getTolerance(),
                        trait.getReverse()
                ))
                .collect(Collectors.toList());
    }
}

