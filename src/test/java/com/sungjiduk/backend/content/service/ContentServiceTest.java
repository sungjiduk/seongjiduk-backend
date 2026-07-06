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
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

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
    }
}
