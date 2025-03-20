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
        part.put("text", "당신은 Java Spring Boot 코드 리뷰 전문가입니다. 코드를 분석하고 다음의 양식에 맞게 리뷰를 작성하세요. 모든 응답은 반드시 한국어로 작성하세요. \n\n" + reviewTemplate() + code);
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

    private StringBuilder reviewTemplate() {
        StringBuilder reviewTemplate = new StringBuilder();
        reviewTemplate.append("# 📢 PR 리뷰 요약\n\n")
                .append("## 1️⃣ PR 개요\n")
                .append("- **제목:** [PR 제목]\n")
                .append("- **기능:** [어떤 기능/버그 수정인지 간략히 설명]\n")
                .append("- **주요 변경 사항:**\n")
                .append("  - [주요 코드 수정 내용 (ex. 함수 추가, 로직 변경, 리팩토링 등)]\n")
                .append("  - [중요한 아키텍처 변경사항이 있다면 설명]\n\n")

                .append("## 2️⃣ 코드 품질 리뷰\n")
                .append("✅ **잘된 점**:\n")
                .append("- [코드의 좋은 점 (ex. 가독성, 모듈화, 재사용성 등)]\n")
                .append("- [효율적인 알고리즘/디자인 패턴 사용 여부]\n\n")

                .append("⚠️ **개선 필요 사항**:\n")
                .append("- [명확하지 않은 코드 또는 개선이 필요한 부분]\n")
                .append("- [성능 최적화가 가능한 부분]\n")
                .append("- [예외 처리 누락, 보안 문제, 사이드 이펙트 발생 가능성]\n\n")

                .append("## 3️⃣ 추가 피드백 & 추천\n")
                .append("💡 **제안하는 개선 사항**:\n")
                .append("- [더 나은 코드 구조/설계 제안]\n")
                .append("- [리팩토링 제안]\n")

                .append("📌 **요청 사항**:\n")
                .append("- [추가 확인이 필요한 부분 (ex. 문서화, 주석 추가 등)]\n")
                .append("- [수정 후 다시 리뷰 요청할지 여부]\n\n")

                .append("✅ **최종 결론**:\n")
                .append("- [🔹 LGTM (Looks Good To Me) / ⏳ Needs Changes / ❌ Request Changes]\n");
        return reviewTemplate;
    }
}
