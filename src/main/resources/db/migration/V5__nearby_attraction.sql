-- 주변 관광 명소 스냅샷 (일정에 담긴 것만 저장, TripStop.nearby_attraction_id가 참조)
-- 관련 문서: 기획/04_도메인_모델_ERD.md
CREATE TABLE nearby_attraction (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100)  NOT NULL,
    category VARCHAR(50),
    lat      DECIMAL(10,7) NOT NULL,
    lng      DECIMAL(10,7) NOT NULL,
    maps_url VARCHAR(500)  NOT NULL,
    CONSTRAINT uk_nearby_attraction_maps_url UNIQUE (maps_url)
);
