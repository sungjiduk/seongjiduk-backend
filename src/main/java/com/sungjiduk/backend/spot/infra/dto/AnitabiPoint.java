package com.sungjiduk.backend.spot.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Anitabi {@code /bangumi/{id}/points/detail} 응답의 성지 포인트 하나.
 * {@code geo}는 [위도, 경도] 순서이며, {@code ep}는 숫자("9")와 문자열("第二季11话")이 섞여 오므로 String으로 받는다.
 * {@code image}는 애니 장면 스크린샷 썸네일 URL — 라이선스(CC BY-NC-SA)상 재호스팅 금지, 링크로만 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnitabiPoint(
        String id,
        String name,
        List<Double> geo,
        String ep,
        String origin,
        String originURL,
        String image
) {

    public double lat() {
        return geo.get(0);
    }

    public double lng() {
        return geo.get(1);
    }
}
