package com.sungjiduk.backend.spot.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * ai-service {@code POST /ai/spots/describe} 응답에서 필요한 필드만.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDescribeResult(
        Long contentId,
        String provider,
        List<AiSpotDescription> descriptions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiSpotDescription(
            Long spotId,
            String sceneDescription,
            String specialPoint,
            String koreanName,          // 한국어 표기 이름 (없으면 원어)
            Integer recommendedMinutes  // AI 추정 체류시간(분)
    ) {
    }
}
