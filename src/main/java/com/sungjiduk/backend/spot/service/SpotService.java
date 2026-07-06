package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.spot.dto.request.SpotReportCreateRequest;
import com.sungjiduk.backend.spot.dto.response.NearbyAttractionsResponse;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.dto.response.SpotReportResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.NearbyAttractionsProvider;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpotService {

    private final PilgrimageSpotRepository spotRepository;
    private final NearbyAttractionsProvider attractionsProvider;

    public SpotService(PilgrimageSpotRepository spotRepository, NearbyAttractionsProvider attractionsProvider) {
        this.spotRepository = spotRepository;
        this.attractionsProvider = attractionsProvider;
    }

    /** 성지별 주변 관광지 캐시(외부 API 절약). 빈 결과는 캐시하지 않아 키 추가 시 재시도된다. */
    private final java.util.Map<Long, NearbyAttractionsResponse> nearbyCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_ATTRACTIONS = 6;

    public NearbyAttractionsResponse findNearbyAttractions(Long spotId) {
        NearbyAttractionsResponse cached = nearbyCache.get(spotId);
        if (cached != null) {
            return cached;
        }
        PilgrimageSpot spot = spotRepository.findByIdOrThrow(spotId);
        var attractions = attractionsProvider
                .findNearby(spot.getLat().doubleValue(), spot.getLng().doubleValue())
                .stream()
                .filter(a -> a.rating() != null && a.ratingCount() != null)
                .sorted(java.util.Comparator.comparing(
                        NearbyAttractionsProvider.Attraction::ratingCount).reversed())
                .limit(MAX_ATTRACTIONS)
                .map(a -> new NearbyAttractionsResponse.AttractionSummary(
                        a.name(), a.category(), a.rating(), a.ratingCount(), a.lat(), a.lng(), a.mapsUrl()))
                .toList();
        NearbyAttractionsResponse response = new NearbyAttractionsResponse(spotId, attractions);
        if (!attractions.isEmpty()) {
            nearbyCache.put(spotId, response);
        }
        return response;
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
