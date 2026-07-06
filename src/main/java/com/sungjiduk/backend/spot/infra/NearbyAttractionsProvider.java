package com.sungjiduk.backend.spot.infra;

import java.util.List;

/**
 * 좌표 주변 관광 명소 조회. 스위처블 인터페이스(지오코더 패턴).
 * 키가 없거나 실패하면 빈 리스트 — 호출부는 섹션을 숨기면 된다.
 */
public interface NearbyAttractionsProvider {

    List<Attraction> findNearby(double lat, double lng);

    record Attraction(
            String name,
            String category,        // 예: 신사, 공원, 박물관 (Places type 한국어화)
            Double rating,          // 구글 평점 (없으면 null)
            Integer ratingCount,
            double lat,
            double lng,
            String mapsUrl          // 구글맵 상세 링크
    ) {
    }
}
