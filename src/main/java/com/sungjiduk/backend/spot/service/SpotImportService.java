package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.SpotImportResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.entity.SpotReference;
import com.sungjiduk.backend.spot.infra.AnitabiClient;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder.GeoResult;
import com.sungjiduk.backend.spot.infra.dto.AnitabiPoint;
import com.sungjiduk.backend.spot.infra.dto.AnitabiWork;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.repository.SpotReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Anitabi 성지 포인트를 작품(Content)의 PilgrimageSpot으로 임포트한다.
 * 멱등: {@code (content, name)} 기준 upsert (외부 external_id 컬럼은 A파트 조율 후 도입 예정).
 */
@Service
public class SpotImportService {

    private static final String SOURCE_NAME = "Anitabi";
    /** 애니 장면 스크린샷 링크용 레퍼런스 sourceName (라이선스상 핫링크만, 재호스팅 금지). */
    private static final String SCENE_IMAGE_SOURCE = "Anitabi:scene-image";
    private static final int DEFAULT_DURATION_MIN = 30;

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final SpotReferenceRepository referenceRepository;
    private final AnitabiClient anitabiClient;
    private final ReverseGeocoder reverseGeocoder;

    public SpotImportService(
            ContentRepository contentRepository,
            PilgrimageSpotRepository spotRepository,
            SpotReferenceRepository referenceRepository,
            AnitabiClient anitabiClient,
            ReverseGeocoder reverseGeocoder
    ) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
        this.referenceRepository = referenceRepository;
        this.anitabiClient = anitabiClient;
        this.reverseGeocoder = reverseGeocoder;
    }

    @Transactional
    public SpotImportResponse importSpots(Long contentId, long bangumiId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        AnitabiWork work = anitabiClient.fetchWork(bangumiId);
        List<AnitabiPoint> points = anitabiClient.fetchPoints(bangumiId);

        int created = 0;
        int updated = 0;
        int geocodeFallback = 0;
        int failed = 0;

        for (AnitabiPoint point : points) {
            try {
                BigDecimal lat = BigDecimal.valueOf(point.lat());
                BigDecimal lng = BigDecimal.valueOf(point.lng());

                Optional<GeoResult> geo = reverseGeocoder.reverse(point.lat(), point.lng());
                String address;
                String city;
                if (geo.isPresent()) {
                    address = geo.get().address();
                    city = geo.get().city();
                } else {
                    address = point.name();
                    city = work.city();
                    geocodeFallback++;
                }

                boolean isNew = upsertSpot(content, point, address, lat, lng, city);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (RuntimeException e) {
                failed++;
            }
        }

        return new SpotImportResponse(contentId, bangumiId, created, updated, geocodeFallback, failed, points.size());
    }

    /** @return 새로 만든 성지면 true, 기존 성지 갱신이면 false. */
    private boolean upsertSpot(Content content, AnitabiPoint point, String address, BigDecimal lat, BigDecimal lng, String city) {
        Optional<PilgrimageSpot> existing = spotRepository.findByContentAndName(content, point.name());
        PilgrimageSpot spot;
        boolean isNew;
        if (existing.isPresent()) {
            spot = existing.get();
            spot.update(content, point.name(), address, lat, lng, city, spot.getRecommendedDurationMin(), point.originURL());
            isNew = false;
        } else {
            spot = PilgrimageSpot.create(content, point.name(), address, lat, lng, city, DEFAULT_DURATION_MIN, point.originURL());
            spotRepository.save(spot);
            isNew = true;
        }
        upsertReference(spot, point);
        upsertSceneImage(spot, point);
        return isNew;
    }

    private void upsertReference(PilgrimageSpot spot, AnitabiPoint point) {
        String title = referenceTitle(point);
        referenceRepository.findBySpotAndSourceName(spot, SOURCE_NAME)
                .ifPresentOrElse(
                        reference -> reference.update(title, point.originURL(), SOURCE_NAME),
                        () -> referenceRepository.save(SpotReference.create(spot, title, point.originURL(), SOURCE_NAME))
                );
    }

    private void upsertSceneImage(PilgrimageSpot spot, AnitabiPoint point) {
        if (point.image() == null || point.image().isBlank()) {
            return;
        }
        String title = referenceTitle(point);
        referenceRepository.findBySpotAndSourceName(spot, SCENE_IMAGE_SOURCE)
                .ifPresentOrElse(
                        reference -> reference.update(title, point.image(), SCENE_IMAGE_SOURCE),
                        () -> referenceRepository.save(SpotReference.create(spot, title, point.image(), SCENE_IMAGE_SOURCE))
                );
    }

    private String referenceTitle(AnitabiPoint point) {
        return point.ep() == null || point.ep().isBlank()
                ? point.name()
                : point.name() + " EP" + point.ep();
    }
}
