package com.sungjiduk.backend.spot.controller;

import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.spot.dto.request.SpotReportCreateRequest;
import com.sungjiduk.backend.spot.dto.response.NearbyAttractionsResponse;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.dto.response.SpotReportResponse;
import com.sungjiduk.backend.spot.service.SpotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpotController {

    private final SpotService spotService;

    public SpotController(SpotService spotService) {
        this.spotService = spotService;
    }

    @GetMapping("/api/spots/{spotId}")
    public ApiResponse<SpotDetailResponse> spot(@PathVariable Long spotId) {
        return ApiResponse.ok(spotService.findSpot(spotId));
    }

    /** 성지 주변 관광 명소 (Google Places, 키 없으면 빈 목록 — 프론트는 섹션 숨김) */
    @GetMapping("/api/spots/{spotId}/nearby-attractions")
    public ApiResponse<NearbyAttractionsResponse> nearbyAttractions(@PathVariable Long spotId) {
        return ApiResponse.ok(spotService.findNearbyAttractions(spotId));
    }

    @PostMapping("/api/spot-reports")
    public ApiResponse<SpotReportResponse> report(@Valid @RequestBody SpotReportCreateRequest request) {
        return ApiResponse.ok(spotService.createReport(request));
    }
}
