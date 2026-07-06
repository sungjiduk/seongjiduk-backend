package com.sungjiduk.backend.trip.service;

import com.sungjiduk.backend.attraction.entity.NearbyAttraction;
import com.sungjiduk.backend.attraction.repository.NearbyAttractionRepository;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
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
import com.sungjiduk.backend.trip.infra.AiTripClient;
import com.sungjiduk.backend.trip.infra.dto.AiTripLayout;
import com.sungjiduk.backend.trip.infra.dto.AiTripRequest;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripPlanRepository tripPlanRepository;
    private final PilgrimageSpotRepository spotRepository;
    private final ContentRepository contentRepository;
    private final NearbyAttractionRepository attractionRepository;
    private final AiTripClient aiTripClient;

    public TripService(
            TripPlanRepository tripPlanRepository,
            PilgrimageSpotRepository spotRepository,
            ContentRepository contentRepository,
            NearbyAttractionRepository attractionRepository,
            AiTripClient aiTripClient
    ) {
        this.tripPlanRepository = tripPlanRepository;
        this.spotRepository = spotRepository;
        this.contentRepository = contentRepository;
        this.attractionRepository = attractionRepository;
        this.aiTripClient = aiTripClient;
    }

    @Transactional
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

        layoutRoute(plan, request);

        TripPlan saved = tripPlanRepository.save(plan);
        return toResponse(saved);
    }

    /**
     * 일정 배치. ai-service(LangGraph)를 우선 호출하고, 실패하면 로컬 규칙으로 폴백한다.
     * 기존 Day는 비우고 다시 채우므로 generate/regenerate가 공유한다.
     */
    private void layoutRoute(TripPlan plan, TripGenerateRequest request) {
        Map<Long, PilgrimageSpot> spotsById = loadCandidateSpots(request);
        List<NearbyAttraction> attractions = resolveAttractions(request);
        try {
            AiTripLayout layout = aiTripClient.generate(toAiRequest(plan, request, spotsById, attractions));
            applyAiLayout(plan, layout);
        } catch (RuntimeException e) {
            log.warn("ai-service 일정 생성 실패, 로컬 배치로 폴백합니다: {}", e.getMessage());
            applyLocalLayout(plan, request, spotsById, attractions);
        }
    }

    /** 새로 담은 관광지는 mapsUrl 멱등 upsert로 영속화하고, 재생성용 id들은 로드해 합친다. */
    private List<NearbyAttraction> resolveAttractions(TripGenerateRequest request) {
        Map<Long, NearbyAttraction> merged = new LinkedHashMap<>();
        if (request.attractions() != null) {
            for (TripGenerateRequest.AttractionInput input : request.attractions()) {
                if (input.mapsUrl() == null || input.mapsUrl().isBlank()) {
                    continue;
                }
                NearbyAttraction attraction = attractionRepository.findByMapsUrl(input.mapsUrl())
                        .orElseGet(() -> attractionRepository.save(NearbyAttraction.create(
                                input.name(), input.category(),
                                java.math.BigDecimal.valueOf(input.lat()),
                                java.math.BigDecimal.valueOf(input.lng()),
                                input.mapsUrl())));
                merged.put(attraction.getId(), attraction);
            }
        }
        if (request.selectedAttractionIds() != null && !request.selectedAttractionIds().isEmpty()) {
            attractionRepository.findAllById(request.selectedAttractionIds())
                    .forEach(a -> merged.putIfAbsent(a.getId(), a));
        }
        return new ArrayList<>(merged.values());
    }

    /** 선택 스팟(제외 제거)을 선택 순서대로 로드. 이름/도시는 AI 후보·로컬 이름 스냅샷에 쓴다. */
    private Map<Long, PilgrimageSpot> loadCandidateSpots(TripGenerateRequest request) {
        List<Long> selected = request.selectedSpotIds() == null ? List.of() : request.selectedSpotIds();
        Set<Long> excluded = request.excludedSpotIds() == null ? Set.of() : new HashSet<>(request.excludedSpotIds());

        Map<Long, PilgrimageSpot> loaded = new HashMap<>();
        for (PilgrimageSpot spot : spotRepository.findAllById(selected)) {
            loaded.put(spot.getId(), spot);
        }

        Map<Long, PilgrimageSpot> ordered = new LinkedHashMap<>();
        for (Long id : selected) {
            if (!excluded.contains(id)) {
                ordered.put(id, loaded.get(id)); // 아직 임포트 안 된 id면 null (이름 미상)
            }
        }
        return ordered;
    }

    private AiTripRequest toAiRequest(TripPlan plan, TripGenerateRequest request, Map<Long, PilgrimageSpot> spotsById,
                                      List<NearbyAttraction> attractions) {
        String title = contentRepository.findById(request.contentId())
                .map(content -> content.getTitle())
                .orElse(null);

        List<AiTripRequest.CandidateSpot> candidates = new ArrayList<>();
        spotsById.forEach((id, spot) -> {
            if (spot != null) {
                candidates.add(new AiTripRequest.CandidateSpot(
                        spot.getId(), spot.getName(), spot.getCity(),
                        spot.getLat() == null ? null : spot.getLat().doubleValue(),
                        spot.getLng() == null ? null : spot.getLng().doubleValue(),
                        spot.getRecommendedDurationMin(), "PILGRIMAGE"));
            }
        });
        for (NearbyAttraction attraction : attractions) {
            candidates.add(new AiTripRequest.CandidateSpot(
                    attraction.getId(), attraction.getName(), null,
                    attraction.getLat().doubleValue(), attraction.getLng().doubleValue(),
                    30, "ATTRACTION"));
        }

        return new AiTripRequest(
                new AiTripRequest.Content(request.contentId(), title),
                new AiTripRequest.Conditions(
                        request.durationDays(), request.budgetLevel(),
                        request.startLocation(), request.travelStyle()),
                candidates,
                new ArrayList<>(spotsById.keySet()),
                request.excludedSpotIds() == null ? List.of() : request.excludedSpotIds());
    }

    private void applyAiLayout(TripPlan plan, AiTripLayout layout) {
        plan.changeTitle(layout.title());
        plan.getDays().clear();
        List<AiTripLayout.Day> days = layout.days() == null ? List.of() : layout.days();
        for (AiTripLayout.Day aiDay : days) {
            TripDay day = TripDay.builder().dayNo(aiDay.dayNo()).summary(aiDay.summary()).build();
            plan.addDay(day);
            List<AiTripLayout.Stop> stops = aiDay.stops() == null ? List.of() : aiDay.stops();
            for (AiTripLayout.Stop aiStop : stops) {
                SpotType spotType = parseSpotType(aiStop.spotType());
                day.addStop(TripStop.builder()
                        .spotType(spotType)
                        .pilgrimageSpotId(spotType == SpotType.PILGRIMAGE ? aiStop.spotId() : null)
                        .nearbyAttractionId(spotType == SpotType.ATTRACTION ? aiStop.spotId() : null)
                        .sequence(aiStop.sequence())
                        .name(aiStop.name())
                        .arrivalTime(aiStop.arrivalTime())
                        .stayMinutes(aiStop.stayMinutes())
                        .reason(aiStop.reason())
                        .build());
            }
        }
    }

    private SpotType parseSpotType(String raw) {
        try {
            return SpotType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return SpotType.PILGRIMAGE;
        }
    }

    /** ai-service 미가용 시 로컬 라운드로빈 배치(이름 스냅샷만 채우고 이유는 비운다). */
    private void applyLocalLayout(TripPlan plan, TripGenerateRequest request, Map<Long, PilgrimageSpot> spotsById,
                                  List<NearbyAttraction> attractions) {
        plan.getDays().clear();

        List<TripDay> days = new ArrayList<>();
        for (int dayNo = 1; dayNo <= plan.getDurationDays(); dayNo++) {
            TripDay day = TripDay.builder()
                    .dayNo(dayNo)
                    .summary("Day " + dayNo + " 성지순례")
                    .build();
            plan.addDay(day);
            days.add(day);
        }

        List<Long> spotIds = new ArrayList<>(spotsById.keySet());
        for (int i = 0; i < spotIds.size(); i++) {
            Long spotId = spotIds.get(i);
            PilgrimageSpot spot = spotsById.get(spotId);
            TripDay day = days.get(i % days.size());
            int sequence = day.getStops().size() + 1;
            day.addStop(TripStop.builder()
                    .spotType(SpotType.PILGRIMAGE)
                    .pilgrimageSpotId(spotId)
                    .sequence(sequence)
                    .name(spot != null ? spot.getName() : null)
                    .arrivalTime(String.format("%02d:00", 9 + sequence))
                    .stayMinutes(spot != null ? spot.getRecommendedDurationMin() : 30)
                    .build());
        }
        for (int i = 0; i < attractions.size(); i++) {
            NearbyAttraction attraction = attractions.get(i);
            TripDay day = days.get((spotIds.size() + i) % days.size());
            int sequence = day.getStops().size() + 1;
            day.addStop(TripStop.builder()
                    .spotType(SpotType.ATTRACTION)
                    .nearbyAttractionId(attraction.getId())
                    .sequence(sequence)
                    .name(attraction.getName())
                    .arrivalTime(String.format("%02d:00", 9 + sequence))
                    .stayMinutes(30)
                    .build());
        }
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
                                        stop.getName(),
                                        stop.getArrivalTime(),
                                        stop.getStayMinutes(),
                                        stop.getReason()))
                                .toList()))
                .toList();
        return new TripResponse(plan.getId(), plan.getTitle(), days, plan.getShareToken());
    }

    @Transactional
    public TripResponse regenerate(Long tripId, TripGenerateRequest request) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        layoutRoute(plan, request);
        return toResponse(plan);
    }

    public TripSummaryResponse save(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        plan.markSaved();
        tripPlanRepository.save(plan);
        return toSummary(plan);
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> findMyTrips() {
        return tripPlanRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    private TripSummaryResponse toSummary(TripPlan plan) {
        return new TripSummaryResponse(plan.getId(), plan.getTitle(), plan.getDurationDays(), plan.getStatus().name());
    }

    @Transactional(readOnly = true)
    public TripResponse findTrip(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return toResponse(plan);
    }

    @Transactional
    public void delete(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        tripPlanRepository.delete(plan);
    }

    @Transactional
    public TripShareResponse share(Long tripId) {
        TripPlan plan = tripPlanRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        plan.assignShareToken(UUID.randomUUID().toString().replace("-", ""));
        String shareUrl = "https://seongjiduk.example/share/" + plan.getShareToken();
        return new TripShareResponse(plan.getId(), shareUrl, plan.getTitle() + " 공유");
    }
}
