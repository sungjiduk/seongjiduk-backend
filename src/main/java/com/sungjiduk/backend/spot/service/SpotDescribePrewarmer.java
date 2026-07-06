package com.sungjiduk.backend.spot.service;

import com.sungjiduk.backend.content.service.ContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 임포트 직후 AI 설명을 백그라운드로 사전 생성한다.
 * 응답을 막지 않고, 실패해도 임포트 결과에 영향 없음(다음 조회에서 재시도).
 */
@Component
public class SpotDescribePrewarmer {

    private static final Logger log = LoggerFactory.getLogger(SpotDescribePrewarmer.class);

    private final ContentService contentService;

    public SpotDescribePrewarmer(ContentService contentService) {
        this.contentService = contentService;
    }

    @Async
    public void prewarmAsync(Long contentId) {
        try {
            contentService.prewarmDescriptions(contentId);
            log.info("AI 설명 프리웜 완료 contentId={}", contentId);
        } catch (RuntimeException e) {
            log.warn("AI 설명 프리웜 실패(다음 조회에서 재시도) contentId={}: {}", contentId, e.getMessage());
        }
    }
}
