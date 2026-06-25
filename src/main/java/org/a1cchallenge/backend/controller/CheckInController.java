package org.a1cchallenge.backend.controller;

import org.a1cchallenge.backend.dto.CheckInRequest;
import org.a1cchallenge.backend.dto.CheckInResponse;
import org.a1cchallenge.backend.dto.DraftCheckInRequest;
import org.a1cchallenge.backend.service.CheckInService;
import org.a1cchallenge.backend.service.CheckInValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkins")
public class CheckInController {

    private final CheckInService checkInService;
    private final CheckInValidationService validationService;

    public CheckInController(CheckInService checkInService, CheckInValidationService validationService) {
        this.checkInService = checkInService;
        this.validationService = validationService;
    }

    /** Section 7.2: 200 OK with warnings if soft-validation rules are tripped. */
    @PostMapping
    public ResponseEntity<CheckInResponse> submitCheckIn(
            @RequestHeader("X-Participant-Token") String token,
            @Valid @RequestBody CheckInRequest request) {
        request.setToken(token);
        List<String> warnings = validationService.validateSoftRules(request);
        CheckInResponse response = checkInService.processCheckIn(request);
        response.setWarnings(warnings);
        return ResponseEntity.ok(response);
    }

    /** Section 5: retrieve draft for the current week, 404 if none exists. */
    @GetMapping("/draft")
    public ResponseEntity<?> getDraft(
            @RequestHeader("X-Participant-Token") String token,
            @RequestParam Integer studyWeek) {
        return checkInService.getDraft(token, studyWeek)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Section 5: transient draft storage for fill-as-you-go UX. */
    @PostMapping("/draft")
    public ResponseEntity<String> saveDraft(
            @RequestHeader("X-Participant-Token") String token,
            @Valid @RequestBody DraftCheckInRequest request) {
        request.setToken(token);
        checkInService.saveDraft(request);
        return ResponseEntity.ok("Draft saved");
    }

    /** Optional explicit cleanup (e.g. user clicks "Discard"). */
    @DeleteMapping("/draft")
    public ResponseEntity<String> discardDraft(
            @RequestHeader("X-Participant-Token") String token,
            @RequestParam Integer studyWeek) {
        checkInService.discardDraft(token, studyWeek);
        return ResponseEntity.ok("Draft discarded");
    }
}
