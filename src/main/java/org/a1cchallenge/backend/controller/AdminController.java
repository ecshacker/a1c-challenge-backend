package org.a1cchallenge.backend.controller;

import org.a1cchallenge.backend.entity.StudyConfig;
import org.a1cchallenge.backend.entity.StudyStatus;
import org.a1cchallenge.backend.service.StudyConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * "Break glass" study-status control. Protected by a shared secret loaded from
 * the environment (X-Admin-Secret). Never expose without it.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final StudyConfigService studyConfigService;

    @Value("${admin.secret}")
    private String adminSecret;

    public AdminController(StudyConfigService studyConfigService) {
        this.studyConfigService = studyConfigService;
    }

    @PostMapping("/study-status")
    public ResponseEntity<?> updateStudyStatus(
            @RequestHeader("X-Admin-Secret") String secret,
            @RequestParam StudyStatus status,
            @RequestParam String updatedBy) {

        if (secret == null || !secret.equals(adminSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Invalid admin secret"));
        }

        StudyConfig updated = studyConfigService.updateStatus(status, updatedBy);
        return ResponseEntity.ok(Map.of(
                "message", "Study status updated successfully",
                "new_status", updated.getStatus().toString(),
                "updated_at", String.valueOf(updated.getUpdatedAt())
        ));
    }
}
