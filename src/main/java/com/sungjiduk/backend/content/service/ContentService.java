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
import com.sungjiduk.backend.spot.entity.SpotReference;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.repository.SpotReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private static final String SCENE_IMAGE_SOURCE = "Anitabi:scene-image";

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final SpotReferenceRepository referenceRepository;
    private final AiDescribeClient aiDescribeClient;

    /**
     * 성지별 AI 장면 설명 캐시. GPT 호출은 느리고 비용이 들어 같은 성지는 재호출하지 않는다.
     * (설명은 성지·작품 고정 → 서버 재시작 전까지 유효. 상용화 시 DB 컬럼/TTL로 대체.)
     */
    private final Map<Long, AiSpotDescription> descriptionCache = new ConcurrentHashMap<>();

    /** 작품별 describe 호출 락(single-flight). 동시 요청 중 첫 요청만 AI를 부르고 나머지는 캐시를 기다린다. */
    private final Map<Long, Object> describeLocks = new ConcurrentHashMap<>();

    /** 프리웜 전용 모델 (비면 ai-service 기본). */
    private final String prewarmModel;

    public ContentService(
            ContentRepository contentRepository,
            PilgrimageSpotRepository spotRepository,
            SpotReferenceRepository referenceRepository,
            AiDescribeClient aiDescribeClient,
            @org.springframework.beans.factory.annotation.Value("${seongjiduk.ai-service.prewarm-model:}") String prewarmModel
    ) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
        this.referenceRepository = referenceRepository;
        this.aiDescribeClient = aiDescribeClient;
        this.prewarmModel = prewarmModel;
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

        fetchMissingDescriptions(content, spots, null);
        Map<Long, String> sceneImages = sceneImagesBySpotId(spots);

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
                            description == null ? null : description.specialPoint(),
                            sceneImages.get(spot.getId()));
                })
                .toList();
        return new ContentSpotsResponse(content.getId(), content.getTitle(), summaries);
    }

    /** 성지별 애니 장면 이미지 URL(Anitabi 핫링크). 없으면 map에 없음. */
    private Map<Long, String> sceneImagesBySpotId(List<PilgrimageSpot> spots) {
        if (spots.isEmpty()) {
            return Map.of();
        }
        return referenceRepository.findBySpotInAndSourceName(spots, SCENE_IMAGE_SOURCE).stream()
                .collect(java.util.stream.Collectors.toMap(
                        reference -> reference.getSpot().getId(),
                        SpotReference::getUrl,
                        (first, second) -> first));
    }

    /**
     * 캐시에 없는 성지만 ai-service에 설명을 요청한다.
     * 실패해도 목록 조회는 계속돼야 하므로(설명만 null) 예외는 삼킨다.
     */
    /**
     * 임포트 직후 백그라운드 사전 생성용 — 설명 캐시를 미리 채운다.
     * single-flight 락을 공유하므로 사용자 요청과 겹쳐도 GPT는 1회만 호출된다.
     */
    @Transactional(readOnly = true)
    public void prewarmDescriptions(Long contentId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        List<PilgrimageSpot> spots = spotRepository.findByContentOrderByIdAsc(content);
        fetchMissingDescriptions(content, spots, prewarmModel == null || prewarmModel.isBlank() ? null : prewarmModel);
    }

    private void fetchMissingDescriptions(Content content, List<PilgrimageSpot> spots, String model) {
        if (spots.stream().allMatch(spot -> descriptionCache.containsKey(spot.getId()))) {
            return;
        }
        Object lock = describeLocks.computeIfAbsent(content.getId(), id -> new Object());
        synchronized (lock) {
            // 락 획득 후 재확인 — 먼저 들어온 요청이 이미 채웠으면 호출 생략 (StrictMode 이중 fetch 등)
            List<PilgrimageSpot> missing = spots.stream()
                    .filter(spot -> !descriptionCache.containsKey(spot.getId()))
                    .toList();
            if (missing.isEmpty()) {
                return;
            }
            callDescribe(content, missing, model);
        }
    }

    private void callDescribe(Content content, List<PilgrimageSpot> missing, String model) {
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
                            .toList(),
                    model));
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
