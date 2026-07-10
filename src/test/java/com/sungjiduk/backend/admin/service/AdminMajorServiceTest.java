package com.sungjiduk.backend.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sungjiduk.backend.admin.dto.request.AdminMajorContentSpotRequest;
import com.sungjiduk.backend.admin.dto.request.AdminMajorUserRateRequest;
import com.sungjiduk.backend.admin.dto.request.Duration;
import com.sungjiduk.backend.admin.dto.response.AdminMajorContentSpotResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorUserResponse;
import com.sungjiduk.backend.admin.dto.response.AdminStatsOverviewResponse;
import com.sungjiduk.backend.admin.dto.response.StatsSeriesResponse;
import com.sungjiduk.backend.common.config.SecurityConfig;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.event.entity.EventType;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;
import com.sungjiduk.backend.user.repository.UserRepository;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@DisplayName("AdminContentService")
public class AdminMajorServiceTest {
    @Autowired
    AdminMajorService adminMajorService;

    @MockitoBean
    PilgrimageSpotRepository pilgrimageSpotRepository;
    @MockitoBean
    TripStopRepository tripStopRepository;
    @MockitoBean
    TripPlanRepository tripPlanRepository;
    @MockitoBean
    UsageEventRepository usageEventRepository;

    @Nested
    @DisplayName("user는")
    class user {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.week, LocalDate.of(2025, 7, 1)));

            // then
            assertThat(response.values().size() == 7);
            assertThat(response.dates().size() == 7);
        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.month, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 4).isTrue();
            assertThat(response.dates().size() == 4).isTrue();

            String []expected = {"2026-06-2", "2026-06-3", "2026-06-4", "2026-06-5"};

            for (int i = 0; i < 4; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {
            // given
            given(usageEventRepository.countUsageEventByOccurredAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.year, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 12).isTrue();
            assertThat(response.dates().size() == 12).isTrue();

            String []expected = {"2025-08", "2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};

            for (int i = 0; i < 12; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("rate는")
    class rate {
        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {
            // given
            given(tripPlanRepository.countByUserIsNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);
            given(tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.week, LocalDate.of(2025, 7, 1)));

            // then
            assertThat(response.values().size() == 7);
            assertThat(response.dates().size() == 7);
        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {
            // given
            given(tripPlanRepository.countByUserIsNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);
            given(tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.month, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 4).isTrue();
            assertThat(response.dates().size() == 4).isTrue();

            String []expected = {"2026-06-2", "2026-06-3", "2026-06-4", "2026-06-5"};

            for (int i = 0; i < 4; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {
            // given
            given(tripPlanRepository.countByUserIsNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);
            given(tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(3L);

            // when
            AdminMajorUserResponse response = adminMajorService.user(
                new AdminMajorUserRateRequest(Duration.year, LocalDate.of(2026, 7, 1)));

            // then
            assertThat(response.values().size() == 12).isTrue();
            assertThat(response.dates().size() == 12).isTrue();

            String []expected = {"2025-08", "2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};

            for (int i = 0; i < 12; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("content는")
    class content {
        List<Content> mockContent = List.of(
            Content.builder().title("포르자 호라이즌 3").category("GAME").country("호주").description("갓겜").build(),
            Content.builder().title("뉴 슈퍼 마리오 브라더스 DS").category("GAME").country("일본").description("마리오 형제가 피치 공주를 구하러 갑니다.").build(),
            Content.builder().title("네모바지 스펀지밥").category("ANIME").country("미국").description("깊은 저 바닷속 파인애플").build(),
            Content.builder().title("살인의 추억").category("MOVIE").country("한국").description("봉보로봉봉").build(),
            Content.builder().title("사이버펑크 2077").category("GAME").country("폴란드").description("망겜").build()
        );

        void givenEach() {
            given(tripPlanRepository.findMostFrequentContent(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockContent);

            given(tripPlanRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(5L);

            given(tripPlanRepository.countByContentAndCreatedAtBetween(any(Content.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1L);
        }

        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.content(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 7).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 7).isTrue();
        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.content(new AdminMajorContentSpotRequest(
                Duration.month,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 4).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 4).isTrue();

            String []expected = {"2026-06-2", "2026-06-3", "2026-06-4", "2026-06-5"};

            for (int i = 0; i < 4; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.content(new AdminMajorContentSpotRequest(
                Duration.year,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 12).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 12).isTrue();

            String []expected = {"2025-08", "2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};

            for (int i = 0; i < 12; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("count보다 적은 작품수가 주어지면 기타 통계가 잡히지 않아야 한다.")
        void success_small() {
            // given
            List<Content> mockContent2 = List.of(
                Content.builder().title("포르자 호라이즌 3").category("GAME").country("호주").description("갓겜").build(),
                Content.builder().title("뉴 슈퍼 마리오 브라더스 DS").category("GAME").country("일본").description("마리오 형제가 피치 공주를 구하러 갑니다.").build(),
                Content.builder().title("네모바지 스펀지밥").category("ANIME").country("미국").description("깊은 저 바닷속 파인애플").build()
            );

            given(tripPlanRepository.findMostFrequentContent(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockContent2);

            given(tripPlanRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(3L);

            given(tripPlanRepository.countByContentAndCreatedAtBetween(any(Content.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1L);

            // when
            AdminMajorContentSpotResponse response = adminMajorService.content(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2025, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 7).isTrue();
            assertThat(response.values().getFirst().size() == 3).isTrue();
            assertThat(response.dates().size() == 7).isTrue();
        }

        @Test
        @DisplayName("작품이 없으면 집계된 작품이 없어야 한다")
        void success_empty() {
            // given
            List<Content> mockContent2 = List.of();

            given(tripPlanRepository.findMostFrequentContent(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockContent2);

            given(tripPlanRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L);

            given(tripPlanRepository.countByContentAndCreatedAtBetween(any(Content.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L);

            //
            AdminMajorContentSpotResponse response = adminMajorService.content(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2025, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().getFirst().getFirst().name().equals("집계된 작품이 없습니다.")).isTrue();
        }
    }

    @Nested
    @DisplayName("spot은")
    class spot {
        List<Long> mockSpotId = List.of(1L, 2L, 3L, 4L, 5L);

        void givenEach() {
            given(tripStopRepository.findMostFrequentSpotToday(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockSpotId);

            given(tripStopRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(5L);

            given(tripStopRepository.countByPilgrimageSpotIdAndCreatedAtBetween(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1L);

            given(pilgrimageSpotRepository.findById(any(Long.class)))
                .willReturn(Optional.of(PilgrimageSpot.builder()
                    .content(Content.builder().title("포르자 호라이즌 3").category("GAME").country("호주").description("갓겜").build())
                    .name("호라이즌 페스티벌")
                    .address("아웃백")
                    .lat(BigDecimal.valueOf(1))
                    .lng(BigDecimal.valueOf(1))
                    .city("사막 어딘가")
                    .recommendedDurationMin(30)
                    .referenceUrl("www.forza.com")
                    .build())
                );
        }

        @Test
        @DisplayName("일주일 입력이 들어오면 최근 7일의 일간 데이터를 반환해야 한다.")
        void success_week() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.spot(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 7).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 7).isTrue();
        }

        @Test
        @DisplayName("month 입력이 들어오면 최근 4주간 주간 데이터를 반환해야 한다.")
        void success_month() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.spot(new AdminMajorContentSpotRequest(
                Duration.month,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 4).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 4).isTrue();

            String []expected = {"2026-06-2", "2026-06-3", "2026-06-4", "2026-06-5"};

            for (int i = 0; i < 4; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("year 입력이 들어오면 최근 1년간 월간 데이터를 반환해야 한다")
        void success_year() {
            // given
            givenEach();

            // when
            AdminMajorContentSpotResponse response = adminMajorService.spot(new AdminMajorContentSpotRequest(
                Duration.year,
                LocalDate.of(2026, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 12).isTrue();
            assertThat(response.values().getFirst().size() == 5).isTrue();
            assertThat(response.dates().size() == 12).isTrue();

            String []expected = {"2025-08", "2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"};

            for (int i = 0; i < 12; i += 1) {
                assertThat(response.dates().get(i).equals(expected[i])).isTrue();
            }
        }

        @Test
        @DisplayName("count보다 적은 성지 수가 주어지면 기타 통계가 잡히지 않아야 한다.")
        void success_small() {
            // given
            List<Long> mockSpotId = List.of(1L, 2L, 3L);

            given(tripStopRepository.findMostFrequentSpotToday(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockSpotId);

            given(tripStopRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(3L);

            given(tripStopRepository.countByPilgrimageSpotIdAndCreatedAtBetween(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1L);

            given(pilgrimageSpotRepository.findById(any(Long.class)))
                .willReturn(Optional.of(PilgrimageSpot.builder()
                    .content(Content.builder().title("포르자 호라이즌 3").category("GAME").country("호주").description("갓겜").build())
                    .name("호라이즌 페스티벌")
                    .address("아웃백")
                    .lat(BigDecimal.valueOf(1))
                    .lng(BigDecimal.valueOf(1))
                    .city("사막 어딘가")
                    .recommendedDurationMin(30)
                    .referenceUrl("www.forza.com")
                    .build())
                );

            // when
            AdminMajorContentSpotResponse response = adminMajorService.spot(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2025, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().size() == 7).isTrue();
            assertThat(response.values().getFirst().size() == 3).isTrue();
            assertThat(response.dates().size() == 7).isTrue();
        }

        @Test
        @DisplayName("성지가 없으면 집계된 성지가 없어야 한다")
        void success_empty() {
            // given
            List<Long> mockSpotId = List.of();

            given(tripStopRepository.findMostFrequentSpotToday(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockSpotId);

            given(tripStopRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L);

            // when
            AdminMajorContentSpotResponse response = adminMajorService.spot(new AdminMajorContentSpotRequest(
                Duration.week,
                LocalDate.of(2025, 7, 1),
                4L
            ));

            // then
            assertThat(response.values().getFirst().getFirst().name().equals("집계된 성지가 없습니다.")).isTrue();
        }

        @Test
        @DisplayName("잘못된 성지 ID가 들어오면 오류를 반환해야 한다")
        void failed_invalid_spot() {
            // given
            List<Long> mockSpotId = List.of(1L, 2L, 3L);

            given(tripStopRepository.findMostFrequentSpotToday(any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .willReturn(mockSpotId);

            given(tripStopRepository.countAllByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(3L);

            given(tripStopRepository.countByPilgrimageSpotIdAndCreatedAtBetween(any(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1L);

            given(pilgrimageSpotRepository.findById(any(Long.class)))
                .willReturn(Optional.empty());

            //when
            try {
                adminMajorService.spot(new AdminMajorContentSpotRequest(
                    Duration.week,
                    LocalDate.of(2025, 7, 1),
                    4L
                ));
                // then
            } catch (BusinessException e) {
                return;
            }

            assertThat(false).isTrue();
        }
    }
}
