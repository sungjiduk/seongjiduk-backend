package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.content.repository.ContentRepository;
import com.sungjiduk.backend.spot.dto.response.SpotImportResponse;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.entity.SpotReference;
import com.sungjiduk.backend.spot.infra.AnitabiClient;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder;
import com.sungjiduk.backend.spot.infra.ReverseGeocoder.GeoResult;
import com.sungjiduk.backend.spot.infra.dto.AnitabiPoint;
import com.sungjiduk.backend.spot.infra.dto.AnitabiWork;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.spot.repository.SpotReferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@DisplayName("SpotImportService")
class SpotImportServiceTest {

    @Autowired
    private SpotImportService spotImportService;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PilgrimageSpotRepository spotRepository;

    @Autowired
    private SpotReferenceRepository referenceRepository;

    // 외부 호출은 하지 않는다. 클라이언트/지오코더는 BDDMockito로 대체한다.
    @MockitoBean
    private AnitabiClient anitabiClient;

    @MockitoBean
    private ReverseGeocoder reverseGeocoder;

    // 컨텍스트 로딩 시 Redis 연동 회피용.
    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    private Content savedContent() {
        return contentRepository.save(Content.create("러브라이브!", "ANIME", "JP", "테스트 콘텐츠"));
    }

    private AnitabiPoint point(String id, String name, double lat, double lng, String ep) {
        return new AnitabiPoint(id, name, List.of(lat, lng), ep, "Google Maps",
                "https://maps.example/" + id, "https://image.anitabi.cn/points/" + id + ".jpg?plan=h160");
    }

    @Nested
    @DisplayName("importSpots는")
    class ImportSpots {

        @Test
        @DisplayName("Anitabi 포인트를 새 성지로 저장한다")
        void savesNewSpots() {
            // given
            Content content = savedContent();
            given(anitabiClient.fetchWork(49294L)).willReturn(new AnitabiWork("러브라이브!", "千代田区"));
            given(anitabiClient.fetchPoints(49294L)).willReturn(List.of(
                    point("p1", "とんかつ屋さん", 35.7002, 139.7706, "9"),
                    point("p2", "神田郵便局前交差点", 35.6981, 139.7686, "9")
            ));
            given(reverseGeocoder.reverse(anyDouble(), anyDouble()))
                    .willReturn(Optional.of(new GeoResult("東京都千代田区", "千代田区")));

            // when
            SpotImportResponse response = spotImportService.importSpots(content.getId(), 49294L);

            // then
            assertThat(response.created()).isEqualTo(2);
            assertThat(response.updated()).isZero();
            assertThat(response.total()).isEqualTo(2);
            assertThat(spotRepository.findByContentAndName(content, "とんかつ屋さん")).isPresent();
        }

        @Test
        @DisplayName("이름 출처를 SpotReference에 남긴다")
        void savesReference() {
            // given
            Content content = savedContent();
            given(anitabiClient.fetchWork(49294L)).willReturn(new AnitabiWork("러브라이브!", "千代田区"));
            given(anitabiClient.fetchPoints(49294L)).willReturn(List.of(
                    point("p1", "とんかつ屋さん", 35.7002, 139.7706, "9")
            ));
            given(reverseGeocoder.reverse(anyDouble(), anyDouble()))
                    .willReturn(Optional.of(new GeoResult("東京都千代田区", "千代田区")));

            // when
            spotImportService.importSpots(content.getId(), 49294L);

            // then
            PilgrimageSpot spot = spotRepository.findByContentAndName(content, "とんかつ屋さん").orElseThrow();
            SpotReference reference = referenceRepository.findBySpotAndSourceName(spot, "Anitabi").orElseThrow();
            assertThat(reference.getSourceName()).isEqualTo("Anitabi");
            assertThat(reference.getUrl()).isEqualTo("https://maps.example/p1");
        }

        @Test
        @DisplayName("재import 시 같은 이름의 성지를 갱신한다")
        void updatesOnReimport() {
            // given
            Content content = savedContent();
            given(anitabiClient.fetchWork(49294L)).willReturn(new AnitabiWork("러브라이브!", "千代田区"));
            given(anitabiClient.fetchPoints(49294L)).willReturn(List.of(
                    point("p1", "とんかつ屋さん", 35.7002, 139.7706, "9")
            ));
            given(reverseGeocoder.reverse(anyDouble(), anyDouble()))
                    .willReturn(Optional.of(new GeoResult("東京都千代田区", "千代田区")));
            spotImportService.importSpots(content.getId(), 49294L);

            // when
            SpotImportResponse second = spotImportService.importSpots(content.getId(), 49294L);

            // then
            assertThat(second.created()).isZero();
            assertThat(second.updated()).isEqualTo(1);
            assertThat(spotRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("지오코딩 실패 시 작품 city로 fallback한다")
        void fallsBackToWorkCity() {
            // given
            Content content = savedContent();
            given(anitabiClient.fetchWork(49294L)).willReturn(new AnitabiWork("러브라이브!", "千代田区"));
            given(anitabiClient.fetchPoints(49294L)).willReturn(List.of(
                    point("p1", "とんかつ屋さん", 35.7002, 139.7706, "9")
            ));
            given(reverseGeocoder.reverse(anyDouble(), anyDouble())).willReturn(Optional.empty());

            // when
            SpotImportResponse response = spotImportService.importSpots(content.getId(), 49294L);

            // then
            assertThat(response.geocodeFallback()).isEqualTo(1);
            PilgrimageSpot spot = spotRepository.findByContentAndName(content, "とんかつ屋さん").orElseThrow();
            assertThat(spot.getCity()).isEqualTo("千代田区");
            assertThat(spot.getAddress()).isEqualTo("とんかつ屋さん");
        }

        @Test
        @DisplayName("애니 장면 이미지 URL을 별도 SpotReference로 남긴다")
        void savesSceneImageReference() {
            // given
            Content content = savedContent();
            given(anitabiClient.fetchWork(49294L)).willReturn(new AnitabiWork("러브라이브!", "千代田区"));
            given(anitabiClient.fetchPoints(49294L)).willReturn(List.of(
                    point("p1", "とんかつ屋さん", 35.7002, 139.7706, "9")
            ));
            given(reverseGeocoder.reverse(anyDouble(), anyDouble())).willReturn(Optional.empty());

            // when
            spotImportService.importSpots(content.getId(), 49294L);

            // then
            PilgrimageSpot spot = spotRepository.findByContentAndName(content, "とんかつ屋さん").orElseThrow();
            SpotReference imageReference =
                    referenceRepository.findBySpotAndSourceName(spot, "Anitabi:scene-image").orElseThrow();
            assertThat(imageReference.getUrl()).isEqualTo("https://image.anitabi.cn/points/p1.jpg?plan=h160");
        }

        @Test
        @DisplayName("존재하지 않는 content면 CONTENT_NOT_FOUND 예외를 던진다")
        void throwsWhenContentMissing() {
            // given
            long missingContentId = 999_999L;

            // when // then
            assertThatThrownBy(() -> spotImportService.importSpots(missingContentId, 49294L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
        }
    }
}
