package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.spot.dto.request.SpotReportCreateRequest;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.dto.response.SpotReportResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpotService {

    private final PilgrimageSpotRepository spotRepository;

    public SpotService(PilgrimageSpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    public SpotDetailResponse findSpot(Long spotId) {
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        return new SpotDetailResponse(
                spot.getId(),
                spot.getName(),
                spot.getCity(),
                spot.getAddress(),
                spot.getLat().doubleValue(),
                spot.getLng().doubleValue(),
                spot.getRecommendedDurationMin(),
                spot.getReferenceUrl());
    }

    // TODO: SpotReport 엔티티/테이블 확정 후 실제 저장. 현재는 접수 응답만 반환한다.
    public SpotReportResponse createReport(SpotReportCreateRequest request) {
        return new SpotReportResponse(1L, "PENDING");
    }
}
