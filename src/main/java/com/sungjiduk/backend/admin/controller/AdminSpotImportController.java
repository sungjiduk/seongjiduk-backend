package com.sungjiduk.backend.admin.controller;

import com.sungjiduk.backend.admin.dto.request.SpotImportRequest;
import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.spot.dto.response.SpotImportResponse;
import com.sungjiduk.backend.spot.service.SpotDescribePrewarmer;
import com.sungjiduk.backend.spot.service.SpotImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 Anitabi 성지 임포트. {@code /api/admin/**} 는 SecurityConfig에서 ADMIN 전용.
 */
@RestController
@RequestMapping("/api/admin/contents")
public class AdminSpotImportController {

    private final SpotImportService spotImportService;
    private final SpotDescribePrewarmer prewarmer;

    public AdminSpotImportController(SpotImportService spotImportService, SpotDescribePrewarmer prewarmer) {
        this.spotImportService = spotImportService;
        this.prewarmer = prewarmer;
    }

    @PostMapping("/{contentId}/spots/import")
    public ApiResponse<SpotImportResponse> importSpots(
            @PathVariable Long contentId,
            @Valid @RequestBody SpotImportRequest request
    ) {
        SpotImportResponse response = spotImportService.importSpots(contentId, request.bangumiId());
        // 임포트 트랜잭션 커밋 후 백그라운드로 AI 설명 사전 생성 (첫 사용자 대기 제거)
        prewarmer.prewarmAsync(contentId);
        return ApiResponse.ok(response);
    }
}
