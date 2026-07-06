package com.sungjiduk.backend.spot.infra;

import com.sungjiduk.backend.spot.infra.dto.AiDescribeRequest;
import com.sungjiduk.backend.spot.infra.dto.AiDescribeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * ai-service(LangGraph) 성지 장면 설명 호출. 실패는 호출부(ContentService)가
 * "설명 없음" 폴백으로 처리하므로 여기서는 예외를 그대로 전파한다.
 * HTTP/1.1 강제: JDK HttpClient의 h2c 업그레이드를 uvicorn(h11)이 거부하며 body가 유실됨.
 */
@Component
public class AiDescribeClient {

    private final RestClient restClient;

    public AiDescribeClient(@Value("${seongjiduk.ai-service.base-url:http://localhost:8000}") String baseUrl) {
        HttpClient http1Client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(http1Client))
                .build();
    }

    public AiDescribeResult describe(AiDescribeRequest request) {
        return restClient.post()
                .uri("/ai/spots/describe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiDescribeResult.class);
    }
}
