package com.pawever.server.domain.recommendation.matching.entity.dog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_traits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 자동 증가 PK

    @Column(name = "question_id", nullable = false)
    private int questionId;  // 질문 ID

    @Column(name = "option_id", nullable = false)
    private int optionId;  // 선택지 ID

    @Column(name = "trait_name", nullable = false, length = 50)
    private String traitName;  // 특성 이름 (예: energyLevel)

    @Column(name = "score", nullable = false)
    private double score;  // 점수 (1~5)

    @Column(name = "weight", nullable = false)
    private double weight;  // 가중치 (예: 1.0, 0.5)

    @Column(name = "tolerance", nullable = false)
    private Boolean tolerance;  // 허용 여부 (true/false)

    @Column(name = "reverse", nullable = false)
    private Boolean reverse;  // 역가중치 적용 여부
}
