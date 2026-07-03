package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("TripService")
class TripServiceTest {

    @Autowired
    private TripService tripService;

    @Autowired
    private TripPlanRepository tripPlanRepository;

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
}
