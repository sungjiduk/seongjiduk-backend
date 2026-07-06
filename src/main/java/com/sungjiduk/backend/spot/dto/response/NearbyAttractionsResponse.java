package com.sungjiduk.backend.spot.dto.response;

import java.util.List;

/** 성지 주변 관광 명소 (Google Places 기반, 키 없으면 빈 목록). */
public record NearbyAttractionsResponse(
        Long spotId,
        List<AttractionSummary> attractions
) {

    public record AttractionSummary(
            String name,
            String category,
            Double rating,
            Integer ratingCount,
            double lat,
            double lng,
            String mapsUrl
    ) {
    }
}
