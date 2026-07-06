package com.sungjiduk.backend.spot.infra.dto;

import java.util.List;

/**
 * ai-service {@code POST /ai/spots/describe} 요청.
 * ep는 아직 백엔드에 저장하지 않아 null로 보낸다(추후 SpotReference에서 파싱 가능).
 */
public record AiDescribeRequest(
        ContentInfo content,
        List<SceneSpot> spots
) {

    public record ContentInfo(Long id, String title) {
    }

    public record SceneSpot(
            Long id,
            String name,
            String city,
            String address,
            String ep,
            String referenceUrl
    ) {
    }
}
