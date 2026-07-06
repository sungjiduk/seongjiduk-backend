package com.sungjiduk.backend.trip.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 여행 일정(루트)의 최상위 엔티티.
 * user_id / content_id 는 다른 도메인(A파트) 엔티티가 아직 없어
 * 결합을 피하려고 FK 객체가 아닌 Long 값으로 보관한다.
 */
@Entity
@Table(name = "trip_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 비회원 생성 일정은 null 가능 */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_location", length = 200)
    private String startLocation;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    /** DTO와 동일하게 문자열 태그(LOW/NORMAL/HIGH)로 보관 */
    @Column(name = "budget_level", length = 20)
    private String budgetLevel;

    @Column(name = "travel_style", length = 30)
    private String travelStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    /** 공유/비로그인 상세 조회용 토큰 */
    @Column(name = "share_token", length = 64, unique = true)
    private String shareToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tripPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDay> days = new ArrayList<>();

    @Builder
    private TripPlan(Long userId, Long contentId, String title, String startLocation,
                     int durationDays, String budgetLevel, String travelStyle,
                     TripStatus status, String shareToken) {
        this.userId = userId;
        this.contentId = contentId;
        this.title = title;
        this.startLocation = startLocation;
        this.durationDays = durationDays;
        this.budgetLevel = budgetLevel;
        this.travelStyle = travelStyle;
        this.status = status == null ? TripStatus.DRAFT : status;
        this.shareToken = shareToken;
        this.createdAt = LocalDateTime.now();
    }

    /** 양방향 연관관계 편의 메서드 */
    public void addDay(TripDay day) {
        this.days.add(day);
        day.assignPlan(this);
    }

    public void markSaved() {
        this.status = TripStatus.SAVED;
    }

    /** AI가 생성한 제목으로 교체(빈 값이면 유지). */
    public void changeTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }

    /** 공유 토큰을 최초 1회만 발급한다(멱등). 이미 있으면 유지해 공유 링크가 고정되게 한다. */
    public void assignShareToken(String token) {
        if (this.shareToken == null) {
            this.shareToken = token;
        }
    }
}
