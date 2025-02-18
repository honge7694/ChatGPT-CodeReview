package com.gpt.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GitHubService {
    private static final String GITHUB_TOKEN = "";
    private static final String GITHUB_API_URL = "";
    private final String OPENAI_API_KEY = "";
    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> processPullRequest(Map<String, Object> payload) {
        String action = (String) payload.get("action");

        if ("opened".equals(action) || "synchronize".equals(action)) {
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
            String prUrl = (String) pullRequest.get("url");
            String diffUrl = prUrl + "/files";

            // 변경된 코드 가져오기
            List<Map<String, Object>> changedFiles = getGitHubData(diffUrl);

            for (Map<String, Object> file : changedFiles) {
                String fileName = (String) file.get("file_name");
                String patch = (String) file.get("patch");
                log.info("patch : {}", patch);
                // TODO : AI 리뷰 요청, PR에 코멘트 추가
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
}
