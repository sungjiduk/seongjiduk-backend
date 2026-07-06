package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("SpotService")
class SpotServiceTest {

    @Autowired
    private SpotService spotService;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("findSpot은")
    class FindSpot {

        @Test
        @DisplayName("저장된 성지 상세를 좌표와 함께 반환한다")
        void returnsDetail() {
            // given
            Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
            PilgrimageSpot spot = spotRepository.save(PilgrimageSpot.create(
                    content, "とんかつ屋さん", "東京都千代田区",
                    new BigDecimal("35.7002000"), new BigDecimal("139.7706000"),
                    "千代田区", 30, "https://maps.example/p1"));

            // when
            SpotDetailResponse response = spotService.findSpot(spot.getId());

            // then
            assertThat(response.id()).isEqualTo(spot.getId());
            assertThat(response.name()).isEqualTo("とんかつ屋さん");
            assertThat(response.city()).isEqualTo("千代田区");
            assertThat(response.lat()).isEqualTo(35.7002);
            assertThat(response.lng()).isEqualTo(139.7706);
            assertThat(response.recommendedDurationMin()).isEqualTo(30);
        }

        @Test
        @DisplayName("존재하지 않으면 SPOT_NOT_FOUND 예외를 던진다")
        void throwsWhenMissing() {
            // given
            long missingId = 999_999L;

            // when // then
            assertThatThrownBy(() -> spotService.findSpot(missingId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
        }
    }
}
