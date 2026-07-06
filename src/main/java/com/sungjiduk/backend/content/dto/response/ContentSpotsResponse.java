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
            String city,
            String address,
            double lat,
            double lng,
            int recommendedDurationMin,
            String referenceUrl,
            String sceneDescription,   // AI 생성 장면 설명 (ai-service 미가용 시 null)
            String specialPoint        // AI 생성 "특별한 점" (ai-service 미가용 시 null)
    ) {
    }
}
