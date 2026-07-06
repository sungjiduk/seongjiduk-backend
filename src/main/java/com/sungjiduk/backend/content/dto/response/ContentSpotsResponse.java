package com.sungjiduk.backend.content.dto.response;

import java.util.List;

public record ContentSpotsResponse(
        Long contentId,
        String contentTitle,
        List<SpotSummary> spots
) {

    public record SpotSummary(
            Long id,
            String name,
            String koreanName,         // AI 한국어 표기 (미생성 시 null → 프론트는 원어 표시)
            String city,
            String address,
            double lat,
            double lng,
            int recommendedDurationMin,
            String referenceUrl,
            String sceneDescription,   // AI 생성 장면 설명 (ai-service 미가용 시 null)
            String specialPoint,       // AI 생성 "특별한 점" (ai-service 미가용 시 null)
            String sceneImageUrl       // 애니 장면 스크린샷 (Anitabi 핫링크, 없으면 null)
    ) {
    }
}
