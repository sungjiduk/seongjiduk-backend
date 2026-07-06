package com.sungjiduk.backend.content.service;

import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentSummaryResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final PilgrimageSpotRepository spotRepository;

    public ContentService(ContentRepository contentRepository, PilgrimageSpotRepository spotRepository) {
        this.contentRepository = contentRepository;
        this.spotRepository = spotRepository;
    }

    public List<ContentSummaryResponse> findContents() {
        return contentRepository.findAll().stream()
                .map(content -> new ContentSummaryResponse(
                        content.getId(),
                        content.getTitle(),
                        content.getCategory(),
                        content.getCountry()))
                .toList();
    }

    public ContentDetailResponse findContent(Long contentId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        return new ContentDetailResponse(
                content.getId(),
                content.getTitle(),
                content.getCategory(),
                content.getCountry(),
                content.getDescription());
    }

    public ContentSpotsResponse findContentSpots(Long contentId) {
        Content content = contentRepository.findByIdOrThrow(contentId);
        List<ContentSpotsResponse.SpotSummary> spots = spotRepository.findByContentOrderByIdAsc(content).stream()
                .map(spot -> new ContentSpotsResponse.SpotSummary(
                        spot.getId(),
                        spot.getName(),
                        spot.getCity(),
                        spot.getAddress(),
                        spot.getLat().doubleValue(),
                        spot.getLng().doubleValue(),
                        spot.getRecommendedDurationMin(),
                        spot.getReferenceUrl()))
                .toList();
        return new ContentSpotsResponse(content.getId(), content.getTitle(), spots);
    }
}
