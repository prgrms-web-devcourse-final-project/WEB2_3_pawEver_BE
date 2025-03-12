package com.pawever.server.domain.recommendation.matching.entity.cat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_question_traits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatQuestionTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(name = "option_id", nullable = false)
    private Integer optionId;

    @Column(name = "trait_name", nullable = false, length = 50)
    private String traitName;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "tolerance", nullable = true)
    private Boolean tolerance = false;

    @Column(name = "reverse", nullable = true)
    private Boolean reverse = false;
}
