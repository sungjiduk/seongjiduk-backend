package com.sungjiduk.backend.spot.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Google <b>Places API (New)</b> searchNearby 기반 주변 관광 명소.
 * (레거시 nearbysearch는 신규 프로젝트에서 차단 — REQUEST_DENIED 실측)
 * env {@code GOOGLE_MAPS_API_KEY}(Places API (New) 활성화 + billing)가 없거나 실패하면 빈 리스트.
 */
@Component
public class GooglePlacesProvider implements NearbyAttractionsProvider {

    private static final int RADIUS_METERS = 900;
    private static final String FIELD_MASK =
            "places.displayName,places.rating,places.userRatingCount,places.location,"
                    + "places.googleMapsUri,places.primaryTypeDisplayName";

    private final String apiKey;
    private final RestClient restClient;

    public GooglePlacesProvider(@Value("${seongjiduk.geocoding.google.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://places.googleapis.com")
                .build();
    }

    @Override
    public List<Attraction> findNearby(double lat, double lng) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        try {
            SearchNearbyResponse response = restClient.post()
                    .uri("/v1/places:searchNearby")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(Map.of(
                            "includedTypes", List.of("tourist_attraction"),
                            "maxResultCount", 20,
                            "languageCode", "ko",
                            "locationRestriction", Map.of("circle", Map.of(
                                    "center", Map.of("latitude", lat, "longitude", lng),
                                    "radius", RADIUS_METERS))))
                    .retrieve()
                    .body(SearchNearbyResponse.class);
            if (response == null || response.places() == null) {
                return List.of();
            }
            return response.places().stream()
                    .filter(p -> p.displayName() != null && p.location() != null)
                    .map(p -> new Attraction(
                            p.displayName().text(),
                            p.primaryTypeDisplayName() == null ? "명소" : p.primaryTypeDisplayName().text(),
                            p.rating(),
                            p.userRatingCount(),
                            p.location().latitude(),
                            p.location().longitude(),
                            p.googleMapsUri()))
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchNearbyResponse(List<Place> places) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Place(
            LocalizedText displayName,
            LocalizedText primaryTypeDisplayName,
            Double rating,
            Integer userRatingCount,
            LatLng location,
            String googleMapsUri
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocalizedText(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LatLng(double latitude, double longitude) {
    }
}
