package com.gpt.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${token.gemini}")
    private String GEMINI_API_KEY;
    private String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * <p><a href="https://ai.google.dev/gemini-api/docs/get-started/tutorial?lang=rest&hl=ko#gemini_and_content_based_apis">
     * gemini API document
     * </a></p>
     * @param code
     * @return
     */
    public String getAIReview(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-goog-api-key", GEMINI_API_KEY);
        headers.set("Content-Type", "application/json");

        /* Gemini API requestBody
         * -d '{
         * "contents": [{
         *   "parts": [{"text": ""}]
         *  }]
         * }'
         */
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "당신은 Java Spring Boot 코드 리뷰 전문가입니다. 코드를 분석하고 리뷰를 작성하세요. 모든 응답은 반드시 한국어로 작성하세요. \n\n" + code);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (response == null || !responseBody.containsKey("candidates")) {
            return "AI 리뷰를 가져오는데 실패했습니다.";
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "AI 리뷰 응답의 'candidates' 필드가 비어있거나 null입니다.";
        }

        Map<String, Object> candidate = candidates.get(0);
        if (candidate == null || !candidate.containsKey("content")) {
            return "AI 리뷰 응답의 'candidate'가 null이거나 'content' 필드가 없습니다.";
        }

        Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
        if (contentResponse == null || !contentResponse.containsKey("parts")) {
            return "AI 리뷰 응답의 'content'가 null이거나 'parts' 필드가 없습니다.";
        }

        List<Map<String, Object>> partsResponse = (List<Map<String, Object>>) contentResponse.get("parts");
        if (partsResponse == null || partsResponse.isEmpty()) {
            return "AI 리뷰 응답의 'parts' 필드가 비어있거나 null입니다.";
        }

        Map<String, Object> partResponse = partsResponse.get(0);
        if (partResponse == null || !partResponse.containsKey("text")) {
            return "AI 리뷰 응답의 'part'가 null이거나 'text' 필드가 없습니다.";
        }

        String review = (String) partResponse.get("text");
        if (review == null || review.isEmpty()) {
            return "AI 리뷰 응답의 'text' 필드가 비어있거나 null입니다.";
        }

        return review;
    }
}
