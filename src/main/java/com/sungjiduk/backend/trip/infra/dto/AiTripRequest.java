package com.sungjiduk.backend.trip.infra.dto;

import java.util.List;

/**
 * ai-service {@code POST /ai/trips/generate} 요청 바디. ai-service 스키마와 필드명을 맞춘다.
 */
public record AiTripRequest(
        Content content,
        Conditions conditions,
        List<CandidateSpot> candidateSpots,
        List<Long> selectedSpotIds,
        List<Long> excludedSpotIds
) {

    public record Content(Long id, String title) {
    }

    public record Conditions(
            int durationDays,
            String budgetLevel,
            String startLocation,
            String travelStyle
    ) {
    }

    public record CandidateSpot(
            Long id,
            String name,
            String city,
            int recommendedDurationMin
    ) {
    }
}
