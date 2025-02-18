package com.gpt.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubService {
    @Value("${token.github}")
    private String GITHUB_TOKEN;

    @Value("${token.openai}")
    private String OPENAI_API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * pull requests 요청이 발생하면 GITHUB WEBHOOK을 통해 요청을 받음
     * @param payload
     * @return
     */
    public ResponseEntity<String> processPullRequest(Map<String, Object> payload) {
        String action = (String) payload.get("action");

        if ("opened".equals(action) || "synchronize".equals(action)) {
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            String prUrl = (String) pullRequest.get("url");
            String diffUrl = prUrl + "/files";
            log.info("prUrl : {}", prUrl);

            // 변경된 코드 가져오기
            List<Map<String, Object>> changedFiles = getGitHubData(diffUrl);
            for (Map<String, Object> file : changedFiles) {
                String fileName = (String) file.get("filename");
                String patch = (String) file.get("patch");
                log.info("patch : {}", patch);
                log.info("fileName : {}", fileName);

                // AI 리뷰 요청
                String reviewComment = getAIReview(patch);
                // PR에 코멘트 추가
                addReviewComment(prUrl, fileName, reviewComment);
            }
            return ResponseEntity.ok("PR Processed");
        }
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing PR");
    }

    private List<Map<String, Object>> getGitHubData(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody();
    }

    private String getAIReview(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);
        headers.set("Content-Type", "application/json");

        String requestBody = "{ \"model\": \"gpt-3.5\", \"messages\": [ {\"role\": \"system\", \"content\": \"당신은 Java Spring Boot 코드 리뷰 전문가입니다. 코드를 분석하고 리뷰를 작성하세요. 모든 응답은 반드시 한국어로 작성하세요.\"}, {\"role\": \"user\", \"content\": \"Review this code: " + code + "\"} ] }";

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("choices")) {
            return "AI 리뷰를 가져오는 데 실패했습니다.";
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

    private void addReviewComment(String prUrl, String fileName, String comment) {
        String commentsUrl = prUrl + "/comments";

        // 최신 commit_id 가져오기
        String latestCommitId = getLatestCommitId(prUrl);

        // 변경된 파일의 position 가져오기
        int position = getFilePosition(prUrl, fileName);

        Map<String, Object> commentPayload = Map.of(
            "body", "** AI Review : **\n" + comment,
            "path", fileName,
            "commit_id", latestCommitId,
            "position", position
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(commentPayload, headers);
        restTemplate.postForEntity(commentsUrl, entity, String.class);
    }

    private String getLatestCommitId(String prUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(prUrl, HttpMethod.GET, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("head")) {
            throw new RuntimeException("PR 정보를 가져오지 못했습니다.");
        }

        Map<String, Object> head = (Map<String, Object>) responseBody.get("head");
        return (String) head.get("sha"); // 최신 커밋 ID 반환
    }

    private int getFilePosition(String prUrl, String fileName) {
        String filesUrl = prUrl + "/files";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(filesUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> files = response.getBody();
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("PR 파일 정보를 가져오지 못했습니다.");
        }

        for (Map<String, Object> file : files) {
            if (fileName.equals(file.get("filename"))) {
                // patch 정보에서 마지막 변경된 줄을 가져옴
                String patch = (String) file.get("patch");
                if (patch != null) {
                    String[] lines = patch.split("\n");
                    for (int i = lines.length - 1; i >= 0; i--) {
                        if (lines[i].startsWith("@@")) {
                            // "@@ -1,1 +1,1" 형태에서 "+1,1" 부분을 찾아야 함
                            String[] parts = lines[i].split(" ");
                            if (parts.length > 2 && parts[2].startsWith("+")) {
                                String[] lineNumbers = parts[2].substring(1).split(",");
                                return Integer.parseInt(lineNumbers[0]);
                            }
                        }
                    }
                }
            }
        }

        throw new RuntimeException("파일의 변경된 위치를 찾을 수 없습니다.");
    }
}
