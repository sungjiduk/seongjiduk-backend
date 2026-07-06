package com.sungjiduk.backend.trip.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TripGenerateRequest(
        @NotNull Long contentId,
        @Min(1) int durationDays,
        String budgetLevel,
        String startLocation,
        String travelStyle,
        List<Long> selectedSpotIds,
        List<Long> excludedSpotIds,
        List<AttractionInput> attractions,      // 새로 담은 주변 관광지 (mapsUrl 멱등 upsert)
        List<Long> selectedAttractionIds        // 재생성 시 기존 일정의 관광지 유지용
) {

    public record AttractionInput(
            String name,
            String category,
            double lat,
            double lng,
            String mapsUrl
    ) {
    }
}
