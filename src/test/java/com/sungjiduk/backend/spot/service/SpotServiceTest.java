package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.NearbyAttractionsResponse;
import com.sungjiduk.backend.spot.dto.response.SpotDetailResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.infra.NearbyAttractionsProvider;
import com.sungjiduk.backend.spot.infra.NearbyAttractionsProvider.Attraction;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

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

    // 실제 Google Places 호출은 하지 않는다.
    @MockitoBean
    private NearbyAttractionsProvider attractionsProvider;

    private PilgrimageSpot savedSpot() {
        Content content = contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "설명"));
        return spotRepository.save(PilgrimageSpot.create(
                content, "神田明神", "東京都千代田区",
                new BigDecimal("35.7020000"), new BigDecimal("139.7680000"),
                "千代田区", 40, "https://maps.example/kanda"));
    }

    @Nested
    @DisplayName("findNearbyAttractions는")
    class FindNearbyAttractions {

        @Test
        @DisplayName("평점 리뷰수 순으로 상위 6곳을 반환한다")
        void returnsTopAttractionsSorted() {
            // given
            PilgrimageSpot spot = savedSpot();
            given(attractionsProvider.findNearby(anyDouble(), anyDouble())).willReturn(List.of(
                    new Attraction("한적한 카페", "명소", 4.9, 12, 35.70, 139.77, "https://maps/1"),
                    new Attraction("아키하바라 전자상가", "쇼핑", 4.3, 8200, 35.70, 139.77, "https://maps/2"),
                    new Attraction("간다 신사", "신사", 4.5, 5100, 35.70, 139.77, "https://maps/3"),
                    new Attraction("무평점", "명소", null, null, 35.70, 139.77, "https://maps/4"),
                    new Attraction("A", "명소", 4.0, 100, 35.70, 139.77, "https://maps/5"),
                    new Attraction("B", "명소", 4.1, 200, 35.70, 139.77, "https://maps/6"),
                    new Attraction("C", "명소", 4.2, 300, 35.70, 139.77, "https://maps/7"),
                    new Attraction("D", "명소", 4.2, 400, 35.70, 139.77, "https://maps/8")
            ));

            // when
            NearbyAttractionsResponse response = spotService.findNearbyAttractions(spot.getId());

            // then — 리뷰수 내림차순 상위 6, 무평점 제외
            assertThat(response.attractions()).hasSize(6);
            assertThat(response.attractions().get(0).name()).isEqualTo("아키하바라 전자상가");
            assertThat(response.attractions().get(1).name()).isEqualTo("간다 신사");
            assertThat(response.attractions()).extracting(NearbyAttractionsResponse.AttractionSummary::name)
                    .doesNotContain("무평점");
        }

        @Test
        @DisplayName("같은 성지 재조회 시 프로바이더를 다시 호출하지 않는다(캐시)")
        void cachesPerSpot() {
            // given
            PilgrimageSpot spot = savedSpot();
            given(attractionsProvider.findNearby(anyDouble(), anyDouble())).willReturn(List.of(
                    new Attraction("간다 신사", "신사", 4.5, 5100, 35.70, 139.77, "https://maps/3")));

            // when
            spotService.findNearbyAttractions(spot.getId());
            spotService.findNearbyAttractions(spot.getId());

            // then
            then(attractionsProvider).should(times(1)).findNearby(anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("프로바이더가 빈 목록이면(키 없음) 빈 목록을 반환한다")
        void returnsEmptyWhenProviderEmpty() {
            // given
            PilgrimageSpot spot = savedSpot();
            given(attractionsProvider.findNearby(anyDouble(), anyDouble())).willReturn(List.of());

            // when
            NearbyAttractionsResponse response = spotService.findNearbyAttractions(spot.getId());

            // then
            assertThat(response.attractions()).isEmpty();
        }
    }

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
