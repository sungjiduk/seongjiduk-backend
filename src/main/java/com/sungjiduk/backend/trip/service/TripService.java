package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.trip.dto.request.TripGenerateRequest;
import com.sungjiduk.backend.trip.dto.response.TripResponse;
import com.sungjiduk.backend.trip.dto.response.TripShareResponse;
import com.sungjiduk.backend.trip.dto.response.TripSummaryResponse;
import com.sungjiduk.backend.trip.entity.SpotType;
import com.sungjiduk.backend.trip.entity.TripDay;
import com.sungjiduk.backend.trip.entity.TripPlan;
import com.sungjiduk.backend.trip.entity.TripStatus;
import com.sungjiduk.backend.trip.entity.TripStop;
import com.sungjiduk.backend.trip.exception.TripNotFoundException;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TripService {

    private final TripPlanRepository tripPlanRepository;

    public TripService(TripPlanRepository tripPlanRepository) {
        this.tripPlanRepository = tripPlanRepository;
    }

    public TripResponse generate(TripGenerateRequest request) {
        TripPlan plan = TripPlan.builder()
                .contentId(request.contentId())
                .durationDays(request.durationDays())
                .startLocation(request.startLocation())
                .budgetLevel(request.budgetLevel())
                .travelStyle(request.travelStyle())
                .title("성지순례 " + request.durationDays() + "일 루트")
                .status(TripStatus.DRAFT)
                .build();

        List<TripDay> days = new ArrayList<>();
        for (int dayNo = 1; dayNo <= request.durationDays(); dayNo++) {
            TripDay day = TripDay.builder()
                    .dayNo(dayNo)
                    .summary("Day " + dayNo + " 성지순례")
                    .build();
            plan.addDay(day);
            days.add(day);
        }

        Set<Long> excluded = request.excludedSpotIds() == null ? Set.of() : new HashSet<>(request.excludedSpotIds());
        List<Long> spotIds = (request.selectedSpotIds() == null ? List.<Long>of() : request.selectedSpotIds())
                .stream()
                .filter(id -> !excluded.contains(id))
                .toList();
        for (int i = 0; i < spotIds.size(); i++) {
            TripDay day = days.get(i % days.size());
            int sequence = day.getStops().size() + 1;
            day.addStop(TripStop.builder()
                    .spotType(SpotType.PILGRIMAGE)
                    .pilgrimageSpotId(spotIds.get(i))
                    .sequence(sequence)
                    .arrivalTime(String.format("%02d:00", 9 + sequence))
                    .stayMinutes(30)
                    .build());
        }

        TripPlan saved = tripPlanRepository.save(plan);
        return toResponse(saved);
    }

    private TripResponse toResponse(TripPlan plan) {
        List<TripResponse.DayPlan> days = plan.getDays().stream()
                .map(day -> new TripResponse.DayPlan(
                        day.getDayNo(),
                        day.getSummary(),
                        day.getStops().stream()
                                .map(stop -> new TripResponse.Stop(
                                        stop.getSequence(),
                                        stop.getSpotType().name(),
                                        stop.getPilgrimageSpotId() != null
                                                ? stop.getPilgrimageSpotId()
                                                : stop.getNearbyAttractionId(),
                                        null,
                                        stop.getArrivalTime(),
                                        stop.getStayMinutes(),
                                        null))
                                .toList()))
                .toList();
        return new TripResponse(plan.getId(), plan.getTitle(), days, plan.getShareToken());
    }

    public TripResponse regenerate(Long tripId, TripGenerateRequest request) {
        return mockTrip(tripId, request.durationDays());
    }

    public TripSummaryResponse save(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        plan.markSaved();
        tripPlanRepository.save(plan);
        return new TripSummaryResponse(plan.getId(), plan.getTitle(), plan.getDurationDays(), plan.getStatus().name());
    }

    public List<TripSummaryResponse> findMyTrips() {
        return List.of(new TripSummaryResponse(10L, "러브라이브! 뮤즈 성지순례", 3, "SAVED"));
    }

    @Transactional(readOnly = true)
    public TripResponse findTrip(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return toResponse(plan);
    }

    public void delete(Long tripId) {
    }

    public TripShareResponse share(Long tripId) {
        return new TripShareResponse(tripId, "https://seongjiduk.example/trips/" + tripId, "러브라이브! 뮤즈 성지순례 2박 3일 루트");
    }

    private TripResponse mockTrip(Long tripId, int durationDays) {
        return new TripResponse(
                tripId,
                "러브라이브! 뮤즈 " + durationDays + "일 성지순례",
                List.of(new TripResponse.DayPlan(
                        1,
                        "아키하바라 주변 성지 중심 일정",
                        List.of(new TripResponse.Stop(
                                1,
                                "PILGRIMAGE",
                                1L,
                                "쇼헤이바시",
                                "10:00",
                                30,
                                "작품 주요 장면과 연결된 대표 성지입니다."
                        ))
                )),
                "러브라이브! 뮤즈 성지순례 루트"
        );
    }
}
