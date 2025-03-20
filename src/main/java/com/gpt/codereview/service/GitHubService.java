package com.gpt.codereview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {
    @Value("${token.github}")
    private String GITHUB_TOKEN;

    private final RestTemplate restTemplate = new RestTemplate();
    private final OpenAIService openAIService;
    private final GeminiService geminiService;

    /**
     * GitHub Webhook을 통해 Pull Request 이벤트를 처리하는 메서드.
     * <p>GitHub에서 Pull Request(Pull Request Open, Synchronize 등) 이벤트가 발생하면
     * 해당 Webhook 요청을 받아 수행한다.</p>
     * @param payload GitHub Webhook에서 전달된 JSON 데이터의 매핑 객체
     * @return ResponseEntity (예: 성공 시 HTTP 200 OK)
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
                //String reviewComment = openAIService.getAIReview(patch);

                String reviewComment = geminiService.getAIReview(patch);

                // PR에 코멘트 추가
                addReviewComment(prUrl, fileName, reviewComment);
            }
            return ResponseEntity.ok("PR Processed");
        }
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing PR");
    }

    /**
     * Pull Request에서 변경된 파일 목록을 가져온다.
     * <p><a href="https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#list-pull-requests-files">GitHub API - List Pull Request Files</a></p>
     * @param url Pull Request API URL (예: c)
     * @return 변경된 파일 목록을 포함하는 List (각 파일은 Map<String, Object> 형태)
     */
    private List<Map<String, Object>> getGitHubData(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        return response.getBody().stream()
                .filter(file -> !"removed".equals(file.get("status")))
                .collect(Collectors.toList());
    }

    /**
     * Pull Request에서 변경된 코드에 대해 코멘트를 추가한다.
     * @param prUrl Pull Request API URL (예: https://api.github.com/repos/honge7694/ChatGPT-CodeReview/pulls/6)
     * @param fileName 변경된 파일의 이름 (예: src/main/java/com/example/App.java)
     * @param comment AI가 작성한 리뷰
     */
    private void addReviewComment(String prUrl, String fileName, String comment) {
        String commentsUrl = prUrl + "/comments";
        // 최신 commit_id 가져오기
        String latestCommitId = getLatestCommitId(prUrl);
        // 변경된 파일의 position 가져오기
        int position = getFilePosition(prUrl, fileName);

        Map<String, Object> commentPayload = Map.of(
            "body", "💡AI Review\n" + comment,
            "path", fileName,
            "commit_id", latestCommitId,
            "line", position,
            "side", "RIGHT"
        );
        log.info("commentPayload : {}", commentPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + GITHUB_TOKEN);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(commentPayload, headers);
        restTemplate.postForEntity(commentsUrl, entity, String.class);
    }

    /**
     * 최신 커밋 id를 가져온다.
     * <p><a href="https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#get-a-pull-request">GitHub API - Get a pull request</a></p>
     * @param prUrl Pull Request API URL (예: https://api.github.com/repos/honge7694/ChatGPT-CodeReview/pulls/6)
     * @return 최신 커밋 ID 반환 (예: 6dcb09b5b57875f334f61aebed695e2e4193db5e)
     * @throws RuntimeException 응답 데이터가 `null`이거나 "head" 키가 존재하지 않을 경우 발생
     */
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

    /**
     * 변경된 파일의 position(변경된 라인의 시작 위치)을 가져온다.
     * <p><a href="https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#list-pull-requests-files">GitHub API - List Pull Request Files</a></p>
     * @param prUrl Pull Request API URL (예: https://api.github.com/repos/honge7694/ChatGPT-CodeReview/pulls/6)
     * @param fileName 변경된 파일의 이름 (예: src/main/java/com/example/App.java)
     * @return 변경된 라인의 시작 위치 (예: 15 → 해당 파일에서 15번째 라인이 변경됨)
     * @throws RuntimeException 파일 정보를 가져오지 못하거나 변경 위치를 찾을 수 없는 경우 발생
     */
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
                                return Integer.parseInt(lineNumbers[0]) + Integer.parseInt(lineNumbers[1]) - 1;
                            }
                        }
                    }
                }
            }
        }

        throw new RuntimeException("파일의 변경된 위치를 찾을 수 없습니다.");
    }
}
