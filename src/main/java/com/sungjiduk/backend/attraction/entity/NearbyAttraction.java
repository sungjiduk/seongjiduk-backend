package com.sungjiduk.backend.attraction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 성지 주변 관광 명소 스냅샷 (Google Places 결과 중 사용자가 일정에 담은 것만 영속화).
 * TripStop.nearbyAttractionId가 참조한다. 멱등 키 = mapsUrl.
 */
@Entity
@Table(name = "nearby_attraction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NearbyAttraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "maps_url", nullable = false, length = 500, unique = true)
    private String mapsUrl;

    private NearbyAttraction(String name, String category, BigDecimal lat, BigDecimal lng, String mapsUrl) {
        this.name = name;
        this.category = category;
        this.lat = lat;
        this.lng = lng;
        this.mapsUrl = mapsUrl;
    }

    public static NearbyAttraction create(String name, String category, BigDecimal lat, BigDecimal lng, String mapsUrl) {
        return new NearbyAttraction(name, category, lat, lng, mapsUrl);
    }
}
