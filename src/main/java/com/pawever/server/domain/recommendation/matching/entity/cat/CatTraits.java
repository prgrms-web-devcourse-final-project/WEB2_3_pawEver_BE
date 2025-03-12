package com.pawever.server.domain.recommendation.matching.entity.cat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_traits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatTraits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 자동 증가 PK

    @Column(nullable = false, length = 50)
    private String breed; // 고양이 품종

    private Integer adaptability; // 적응력
    private Integer childFriendly; // 아이 친화도
    private Integer sheddingLevel; // 털 빠짐 정도
    private Integer healthIssues; // 건강 문제
    private Integer affectionate; // 애정도
    private Integer intelligence; // 지능
    private Integer energyLevel; // 에너지 레벨
    private Integer socialNeeds; // 사회성
    private Integer grooming; // 관리 필요도
    private Integer strangerFriendly; // 낯선 사람 친화도
    private Integer dogFriendly; // 강아지 친화도
}
