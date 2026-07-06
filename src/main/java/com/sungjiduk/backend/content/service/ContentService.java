package com.sungjiduk.backend.content.service;

import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentSummaryResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.AiDescribeClient;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeRequest;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult.AiSpotDescription;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final AiDescribeClient aiDescribeClient;

    /**
     * 성지별 AI 장면 설명 캐시. GPT 호출은 느리고 비용이 들어 같은 성지는 재호출하지 않는다.
     * (설명은 성지·작품 고정 → 서버 재시작 전까지 유효. 상용화 시 DB 컬럼/TTL로 대체.)
     */
    private final Map<Long, AiSpotDescription> descriptionCache = new ConcurrentHashMap<>();

    public ContentService(
            ContentRepository contentRepository,
            PilgrimageSpotRepository spotRepository,
            AiDescribeClient aiDescribeClient
    ) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
        this.aiDescribeClient = aiDescribeClient;
    }

    public List<ContentSummaryResponse> findContents() {
        return contentRepository.findAll().stream()
                .map(content -> new ContentSummaryResponse(
                        content.getId(),
                        content.getTitle(),
                        content.getCategory(),
                        content.getCountry()))
                .toList();
    }

    public ContentDetailResponse findContent(Long contentId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        return new ContentDetailResponse(
                content.getId(),
                content.getTitle(),
                content.getCategory(),
                content.getCountry(),
                content.getDescription());
    }

    public ContentSpotsResponse findContentSpots(Long contentId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        List<PilgrimageSpot> spots = spotRepository.findByContentOrderByIdAsc(content);

        fetchMissingDescriptions(content, spots);

        List<ContentSpotsResponse.SpotSummary> summaries = spots.stream()
                .map(spot -> {
                    AiSpotDescription description = descriptionCache.get(spot.getId());
                    return new ContentSpotsResponse.SpotSummary(
                            spot.getId(),
                            spot.getName(),
                            spot.getCity(),
                            spot.getAddress(),
                            spot.getLat().doubleValue(),
                            spot.getLng().doubleValue(),
                            spot.getRecommendedDurationMin(),
                            spot.getReferenceUrl(),
                            description == null ? null : description.sceneDescription(),
                            description == null ? null : description.specialPoint());
                })
                .toList();
        return new ContentSpotsResponse(content.getId(), content.getTitle(), summaries);
    }

    /**
     * 캐시에 없는 성지만 ai-service에 설명을 요청한다.
     * 실패해도 목록 조회는 계속돼야 하므로(설명만 null) 예외는 삼킨다.
     */
    private void fetchMissingDescriptions(Content content, List<PilgrimageSpot> spots) {
        List<PilgrimageSpot> missing = spots.stream()
                .filter(spot -> !descriptionCache.containsKey(spot.getId()))
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        try {
            AiDescribeResult result = aiDescribeClient.describe(new AiDescribeRequest(
                    new AiDescribeRequest.ContentInfo(content.getId(), content.getTitle()),
                    missing.stream()
                            .map(spot -> new AiDescribeRequest.SceneSpot(
                                    spot.getId(),
                                    spot.getName(),
                                    spot.getCity(),
                                    spot.getAddress(),
                                    null,
                                    spot.getReferenceUrl()))
                            .toList()));
            if (result == null || result.descriptions() == null) {
                return;
            }
            result.descriptions().stream()
                    .filter(description -> description.spotId() != null)
                    .forEach(description -> descriptionCache.put(description.spotId(), description));
        } catch (RuntimeException e) {
            // ai-service 미가용 → 설명 없이 목록 반환 (다음 조회에서 재시도)
        }
    }
}
