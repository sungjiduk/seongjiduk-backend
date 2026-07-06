package com.sungjiduk.backend.trip.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * ai-service {@code POST /ai/trips/generate} 응답. 도구 노드가 배치한 일자별 stop과
 * LLM 노드가 만든 제목·요약·이유·공유문구를 담는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiTripLayout(
        String title,
        List<Day> days,
        String shareText,
        String provider
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Day(
            int dayNo,
            String summary,
            List<Stop> stops
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stop(
            int sequence,
            String spotType,
            Long spotId,
            String name,
            String arrivalTime,
            int stayMinutes,
            String reason
    ) {
    }
}
