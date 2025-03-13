package com.gpt.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAIService {
    @Value("${token.openai}")
    private String OPENAI_API_KEY;
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAIReview(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);
        headers.set("Content-Type", "application/json");
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 Java Spring Boot 코드 리뷰 전문가입니다. 코드를 분석하고 리뷰를 작성하세요. 모든 응답은 반드시 한국어로 작성하세요."),
                        Map.of("role", "user", "content", "다음 코드를 리뷰해 주세요 : \n```java\n" + code)
                )
//          , "max_tokens", 500 // 비용 줄이기
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("choices")) {
            return "AI 리뷰를 가져오는데 실패했습니다.";
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices.isEmpty()) {
            return "AI 리뷰 결과가 없습니다.";
        }

        Map<String, Object> firstChoice = choices.get(0);
        if (!firstChoice.containsKey("message")) {
            return "AI 리뷰 메시지가 없습니다.";
        }

        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return message.getOrDefault("content", "AI 리뷰 결과를 가져오는 데 실패했습니다.").toString();
    }
}
