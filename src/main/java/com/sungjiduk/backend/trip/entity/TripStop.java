package com.sungjiduk.backend.trip.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루 일정 내 개별 방문 장소.
 * spotType 에 따라 pilgrimageSpotId 또는 nearbyAttractionId 중 하나만 채운다.
 * (DB CHECK 제약 + 애플리케이션 검증으로 보장 — 04_도메인_모델_ERD 설계메모 참고)
 */
@Entity
@Table(name = "trip_stop")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 20)
    private SpotType spotType;

    @Column(name = "pilgrimage_spot_id")
    private Long pilgrimageSpotId;

    @Column(name = "nearby_attraction_id")
    private Long nearbyAttractionId;

    @Column(name = "seq_no", nullable = false)
    private int sequence;

    /** 방문 장소 이름 스냅샷(응답·공유용). AI/로컬 배치 시 채운다. */
    @Column(length = 100)
    private String name;

    /** MVP: mock 시간 문자열(예: "10:00") */
    @Column(name = "arrival_time", length = 10)
    private String arrivalTime;

    @Column(name = "stay_minutes")
    private int stayMinutes;

    /** AI가 생성한 방문 이유. 로컬 폴백 배치에서는 null. */
    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    private TripStop(SpotType spotType, Long pilgrimageSpotId, Long nearbyAttractionId,
                    int sequence, String name, String arrivalTime, int stayMinutes, String reason) {
        this.spotType = spotType;
        this.pilgrimageSpotId = pilgrimageSpotId;
        this.nearbyAttractionId = nearbyAttractionId;
        this.sequence = sequence;
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.stayMinutes = stayMinutes;
        this.reason = reason;
    }

    void assignDay(TripDay tripDay) {
        this.tripDay = tripDay;
    }
}
