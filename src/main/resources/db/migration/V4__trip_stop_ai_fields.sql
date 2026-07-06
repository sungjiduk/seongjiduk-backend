-- Trip stop에 AI/배치 결과 필드 추가
-- name  = 방문 장소 이름 스냅샷(응답·공유용)
-- reason = ai-service(LangGraph LLM 노드)가 생성한 방문 이유(로컬 폴백 시 NULL)
ALTER TABLE trip_stop
    ADD COLUMN name   VARCHAR(100),
    ADD COLUMN reason VARCHAR(500);
