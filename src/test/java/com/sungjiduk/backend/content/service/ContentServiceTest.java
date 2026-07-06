package com.sungjiduk.backend.content.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.dto.response.ContentDetailResponse;
import com.sungjiduk.backend.content.dto.response.ContentSpotsResponse;
import com.sungjiduk.backend.content.dto.response.ContentSummaryResponse;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.entity.SpotReference;
import com.sungjiduk.backend.spot.infra.AiDescribeClient;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult.AiSpotDescription;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.repository.SpotReferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@SpringBootTest
@Transactional
@DisplayName("ContentService")
class ContentServiceTest {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @Autowired
    private SpotReferenceRepository referenceRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    // ai-service 호출은 실제로 하지 않는다.
    @MockitoBean
    private AiDescribeClient aiDescribeClient;

    private Content saveContent(String title) {
        return contentRepository.save(Content.create(title, "ANIME", "JP", title + " 설명"));
    }

    @Nested
    @DisplayName("findContents는")
    class FindContents {

        @Test
        @DisplayName("저장된 작품을 요약 목록으로 반환한다")
        void returnsSavedContents() {
            // given
            saveContent("러브라이브!");
            saveContent("케이온!");

            // when
            List<ContentSummaryResponse> contents = contentService.findContents();

            // then
            assertThat(contents).extracting(ContentSummaryResponse::title)
                    .contains("러브라이브!", "케이온!");
        }
    }

    @Nested
    @DisplayName("findContent는")
    class FindContent {

        @Test
        @DisplayName("작품 상세를 반환한다")
        void returnsDetail() {
            // given
            Content content = saveContent("러브라이브!");

            // when
            ContentDetailResponse detail = contentService.findContent(content.getId());

            // then
            assertThat(detail.id()).isEqualTo(content.getId());
            assertThat(detail.title()).isEqualTo("러브라이브!");
            assertThat(detail.description()).isEqualTo("러브라이브! 설명");
        }

        @Test
        @DisplayName("존재하지 않으면 CONTENT_NOT_FOUND 예외를 던진다")
        void throwsWhenMissing() {
            // given
            long missingId = 999_999L;

            // when // then
            assertThatThrownBy(() -> contentService.findContent(missingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findContentSpots는")
    class FindContentSpots {

        @Test
        @DisplayName("작품에 속한 성지들을 좌표와 함께 반환한다")
        void returnsSpots() {
            // given
            Content content = saveContent("러브라이브!");
            spotRepository.save(PilgrimageSpot.create(
                    content, "とんかつ屋さん", "東京都千代田区",
                    new BigDecimal("35.7002000"), new BigDecimal("139.7706000"),
                    "千代田区", 30, "https://maps.example/p1"));

            // when
            ContentSpotsResponse response = contentService.findContentSpots(content.getId());

            // then
            assertThat(response.contentId()).isEqualTo(content.getId());
            assertThat(response.contentTitle()).isEqualTo("러브라이브!");
            assertThat(response.spots()).hasSize(1);
            ContentSpotsResponse.SpotSummary spot = response.spots().get(0);
            assertThat(spot.name()).isEqualTo("とんかつ屋さん");
            assertThat(spot.lat()).isEqualTo(35.7002);
            assertThat(spot.lng()).isEqualTo(139.7706);
        }

        @Test
        @DisplayName("존재하지 않는 작품이면 CONTENT_NOT_FOUND 예외를 던진다")
        void throwsWhenContentMissing() {
            // given
            long missingId = 999_999L;

            // when // then
            assertThatThrownBy(() -> contentService.findContentSpots(missingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        }

        @Test
        @DisplayName("성지의 애니 장면 이미지 URL을 함께 반환한다")
        void returnsSceneImageUrl() {
            // given
            Content content = saveContent("러브라이브!");
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都千代田区",
                    new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, "https://maps.example/kanda"));
            referenceRepository.save(SpotReference.create(
                    spot, "神田明神 EP1", "https://image.anitabi.cn/points/k1.jpg?plan=h160", "Anitabi:scene-image"));

            // when
            ContentSpotsResponse response = contentService.findContentSpots(content.getId());

            // then
            assertThat(response.spots().get(0).sceneImageUrl())
                    .isEqualTo("https://image.anitabi.cn/points/k1.jpg?plan=h160");
        }

        @Test
        @DisplayName("ai-service의 장면 설명을 성지 목록에 병합한다")
        void mergesAiDescriptions() {
            // given
            Content content = saveContent("러브라이브!");
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都千代田区",
                    new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, "https://maps.example/kanda"));
            given(aiDescribeClient.describe(any())).willReturn(new AiDescribeResult(
                    content.getId(), "openai",
                    List.of(new AiSpotDescription(spot.getId(), "에피소드 1의 배경", "전통 신사의 분위기"))));

            // when
            ContentSpotsResponse response = contentService.findContentSpots(content.getId());

            // then
            ContentSpotsResponse.SpotSummary summary = response.spots().get(0);
            assertThat(summary.sceneDescription()).isEqualTo("에피소드 1의 배경");
            assertThat(summary.specialPoint()).isEqualTo("전통 신사의 분위기");
        }

        @Test
        @DisplayName("ai-service 실패 시 설명 없이(null) 목록을 반환한다")
        void fallsBackToNullDescriptionsOnAiFailure() {
            // given
            Content content = saveContent("러브라이브!");
            spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都千代田区",
                    new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, "https://maps.example/kanda"));
            given(aiDescribeClient.describe(any())).willThrow(new RestClientException("ai-service down"));

            // when
            ContentSpotsResponse response = contentService.findContentSpots(content.getId());

            // then
            assertThat(response.spots()).hasSize(1);
            assertThat(response.spots().get(0).sceneDescription()).isNull();
            assertThat(response.spots().get(0).specialPoint()).isNull();
        }

        @Test
        @DisplayName("같은 성지 재조회 시 ai-service를 다시 호출하지 않는다(캐시)")
        void cachesDescriptionsPerSpot() {
            // given
            Content content = saveContent("러브라이브!");
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "神田明神", "東京都千代田区",
                    new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                    "千代田区", 40, "https://maps.example/kanda"));
            given(aiDescribeClient.describe(any())).willReturn(new AiDescribeResult(
                    content.getId(), "openai",
                    List.of(new AiSpotDescription(spot.getId(), "에피소드 1의 배경", "전통 신사의 분위기"))));

            // when
            contentService.findContentSpots(content.getId());
            ContentSpotsResponse second = contentService.findContentSpots(content.getId());

            // then
            then(aiDescribeClient).should(times(1)).describe(any());
            assertThat(second.spots().get(0).sceneDescription()).isEqualTo("에피소드 1의 배경");
        }
    }
}
