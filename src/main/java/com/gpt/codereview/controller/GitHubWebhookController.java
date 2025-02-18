package com.gpt.codereview.controller;

import com.gpt.codereview.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhook")
public class GitHubWebhookController {

    private final GitHubService gitHubService;

    @PostMapping("/")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        return gitHubService.processPullRequest(payload);
    }
}
