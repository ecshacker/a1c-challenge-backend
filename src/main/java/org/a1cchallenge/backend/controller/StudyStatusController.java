package org.a1cchallenge.backend.controller;

import org.a1cchallenge.backend.service.StudyConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public endpoint — no auth required — so the frontend can gate enrollment. */
@RestController
@RequestMapping("/api/v1/study")
public class StudyStatusController {

    private final StudyConfigService studyConfigService;

    public StudyStatusController(StudyConfigService studyConfigService) {
        this.studyConfigService = studyConfigService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", studyConfigService.getCurrentStatus().name()));
    }
}
