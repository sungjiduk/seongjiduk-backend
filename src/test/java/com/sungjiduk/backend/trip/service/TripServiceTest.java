package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.infra.AiTripClient;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest
@Transactional
@DisplayName("TripService")
class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripPlanRepository tripPlanRepository;

    // 기본은 ai-service 미가용 → 로컬 폴백 경로를 결정론적으로 검증한다.
    // AI 성공 경로 테스트에서만 willReturn으로 재정의한다.
    @MockitoBean
    private AiTripClient aiTripClient;

    @BeforeEach
    void aiServiceDownByDefault() {
        willThrow(new RuntimeException("ai-service down")).given(aiTripClient).generate(any());
    }

    @Nested
    @DisplayName("generate는")
    class Generate {

        @Test
        @DisplayName("일정을 DRAFT 상태로 저장한다")
        void savesDraftPlan() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 3, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(1L, 2L, 3L), List.of());

            // when
            TripResponse response = tripService.generate(request);

            // then
            assertThat(response.tripId()).isNotNull();
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(TripStatus.DRAFT);
            assertThat(saved.getContentId()).isEqualTo(1L);
            assertThat(saved.getDurationDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("선택 스팟을 일자별 PILGRIMAGE stop으로 분배한다")
        void distributesSelectedSpotsAcrossDays() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of());

            // when
            TripResponse response = tripService.generate(request);

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getDays()).hasSize(2);

            List<TripStop> allStops = saved.getDays().stream()
                    .flatMap(day -> day.getStops().stream())
                    .toList();
            assertThat(allStops).allMatch(stop -> stop.getSpotType() == SpotType.PILGRIMAGE);
            assertThat(allStops).extracting(TripStop::getPilgrimageSpotId)
                    .containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        }

        @Test
        @DisplayName("제외 스팟은 일정에서 뺀다")
        void excludesExcludedSpotIds() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L), List.of(20L));

            // when
            TripResponse response = tripService.generate(request);

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            List<Long> spotIds = saved.getDays().stream()
                    .flatMap(day -> day.getStops().stream())
                    .map(TripStop::getPilgrimageSpotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 30L);
        }

        @Test
        @DisplayName("응답에 생성된 Day와 stop을 담아 반환한다")
        void responseReflectsGeneratedDaysAndStops() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of());

            // when
            TripResponse response = tripService.generate(request);

            // then
            assertThat(response.days()).hasSize(2);
            List<Long> spotIds = response.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        }
    }

    @Nested
    @DisplayName("findTrip은")
    class FindTrip {

        @Test
        @DisplayName("저장된 일정의 Day와 stop을 담아 반환한다")
        void returnsTripDetail() {
            // given
            TripResponse created = tripService.generate(new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L, 40L), List.of()));

            // when
            TripResponse found = tripService.findTrip(created.tripId());

            // then
            assertThat(found.tripId()).isEqualTo(created.tripId());
            assertThat(found.days()).hasSize(2);
            List<Long> spotIds = found.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(spotIds).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.findTrip(missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findMyTrips는")
    class FindMyTrips {

        @Test
        @DisplayName("저장된 모든 일정을 요약으로 반환한다")
        void returnsTripSummaries() {
            // given
            tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());
            tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L).durationDays(3).title("뮤즈 3일 루트").status(TripStatus.DRAFT).build());

            // when
            List<TripSummaryResponse> result = tripService.findMyTrips();

            // then
            assertThat(result).extracting(TripSummaryResponse::title)
                    .containsExactlyInAnyOrder("뮤즈 2일 루트", "뮤즈 3일 루트");
            assertThat(result).extracting(TripSummaryResponse::status)
                    .containsExactlyInAnyOrder("SAVED", "DRAFT");
        }

        @Test
        @DisplayName("일정이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNone() {
            // when
            List<TripSummaryResponse> result = tripService.findMyTrips();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save는")
    class Save {

        @Test
        @DisplayName("DRAFT 일정을 SAVED로 전이한다")
        void marksDraftPlanAsSaved() {
            // given
            TripPlan draft = tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L)
                    .durationDays(2)
                    .title("성지순례 2일 루트")
                    .status(TripStatus.DRAFT)
                    .build());

            // when
            TripSummaryResponse response = tripService.save(draft.getId());

            // then
            assertThat(response.tripId()).isEqualTo(draft.getId());
            assertThat(response.durationDays()).isEqualTo(2);
            assertThat(response.status()).isEqualTo("SAVED");
            assertThat(tripPlanRepository.findById(draft.getId()).orElseThrow().getStatus())
                    .isEqualTo(TripStatus.SAVED);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.save(missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete는")
    class Delete {

        @Test
        @DisplayName("저장된 일정을 삭제한다")
        void deletesTrip() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L)
                    .durationDays(2)
                    .title("성지순례 2일 루트")
                    .status(TripStatus.SAVED)
                    .build());

            // when
            tripService.delete(plan.getId());

            // then
            assertThat(tripPlanRepository.findById(plan.getId())).isEmpty();
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.delete(missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("share는")
    class Share {

        @Test
        @DisplayName("일정에 shareToken을 발급하고 공유 응답을 반환한다")
        void issuesShareTokenAndReturnsResponse() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());

            // when
            TripShareResponse response = tripService.share(plan.getId());

            // then
            assertThat(response.tripId()).isEqualTo(plan.getId());
            String token = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();
            assertThat(token).isNotBlank();
            assertThat(response.shareUrl()).contains(token);
        }

        @Test
        @DisplayName("같은 일정을 다시 공유해도 토큰이 유지된다")
        void keepsSameTokenOnReshare() {
            // given
            TripPlan plan = tripPlanRepository.save(TripPlan.builder()
                    .contentId(1L).durationDays(2).title("뮤즈 2일 루트").status(TripStatus.SAVED).build());

            // when
            tripService.share(plan.getId());
            String firstToken = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();
            tripService.share(plan.getId());
            String secondToken = tripPlanRepository.findById(plan.getId()).orElseThrow().getShareToken();

            // then
            assertThat(firstToken).isNotBlank();
            assertThat(secondToken).isEqualTo(firstToken);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            Long missingTripId = 999L;

            // when / then
            assertThatThrownBy(() -> tripService.share(missingTripId))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("regenerate는")
    class Regenerate {

        @Test
        @DisplayName("기존 일정에 추가/제외 스팟을 반영해 재배치한다")
        void reflectsAddedAndExcludedSpots() {
            // given
            TripResponse created = tripService.generate(new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L), List.of()));

            // when — 30 추가, 20 제외
            TripResponse result = tripService.regenerate(created.tripId(), new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L, 20L, 30L), List.of(20L)));

            // then
            assertThat(result.tripId()).isEqualTo(created.tripId());
            List<Long> responseSpotIds = result.days().stream()
                    .flatMap(day -> day.stops().stream())
                    .map(TripResponse.Stop::spotId)
                    .toList();
            assertThat(responseSpotIds).containsExactlyInAnyOrder(10L, 30L);

            List<Long> persistedSpotIds = tripPlanRepository.findById(created.tripId()).orElseThrow()
                    .getDays().stream()
                    .flatMap(day -> day.getStops().stream())
                    .map(TripStop::getPilgrimageSpotId)
                    .toList();
            assertThat(persistedSpotIds).containsExactlyInAnyOrder(10L, 30L);
        }

        @Test
        @DisplayName("없는 일정이면 TripNotFoundException을 던진다")
        void throwsWhenTripNotFound() {
            // given
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 2, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of());

            // when / then
            assertThatThrownBy(() -> tripService.regenerate(999L, request))
                    .isInstanceOf(TripNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ai-service가 응답하면")
    class AiLayout {

        @Test
        @DisplayName("AI가 만든 제목·요약·이유·이름을 일정에 반영한다")
        void appliesAiTitleSummaryReasonAndName() {
            // given
            willReturn(new AiTripLayout(
                    "AI가 다듬은 뮤즈 성지순례",
                    List.of(new AiTripLayout.Day(1, "아키하바라 감성 산책",
                            List.of(new AiTripLayout.Stop(
                                    1, "PILGRIMAGE", 10L, "とんかつ屋さん",
                                    "10:00", 40, "9화 명장면의 무대라 놓칠 수 없어요.")))),
                    "AI가 만든 공유 문구",
                    "openai")).given(aiTripClient).generate(any());
            TripGenerateRequest request = new TripGenerateRequest(
                    1L, 1, "NORMAL", "Tokyo Station", "PILGRIMAGE_ONLY",
                    List.of(10L), List.of());

            // when
            TripResponse response = tripService.generate(request);

            // then
            TripPlan saved = tripPlanRepository.findById(response.tripId()).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("AI가 다듬은 뮤즈 성지순례");
            assertThat(saved.getDays()).hasSize(1);
            assertThat(saved.getDays().get(0).getSummary()).isEqualTo("아키하바라 감성 산책");
            TripStop stop = saved.getDays().get(0).getStops().get(0);
            assertThat(stop.getPilgrimageSpotId()).isEqualTo(10L);
            assertThat(stop.getName()).isEqualTo("とんかつ屋さん");
            assertThat(stop.getReason()).isEqualTo("9화 명장면의 무대라 놓칠 수 없어요.");
            assertThat(response.days().get(0).stops().get(0).reason())
                    .isEqualTo("9화 명장면의 무대라 놓칠 수 없어요.");
        }
    }
}
