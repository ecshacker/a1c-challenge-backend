package com.a1cchallenge.controller;

import com.a1cchallenge.dto.CheckInRequest;
import com.a1cchallenge.dto.CheckInResponse;
import com.a1cchallenge.dto.DraftCheckInRequest;
import com.a1cchallenge.service.CheckInService;
import com.a1cchallenge.service.CheckInValidationService;
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
    public ResponseEntity<CheckInResponse> submitCheckIn(@Valid @RequestBody CheckInRequest request) {
        List<String> warnings = validationService.validateSoftRules(request);
        CheckInResponse response = checkInService.processCheckIn(request);
        response.setWarnings(warnings);
        return ResponseEntity.ok(response);
    }

    /** Section 5: transient draft storage for fill-as-you-go UX. */
    @PostMapping("/draft")
    public ResponseEntity<String> saveDraft(@Valid @RequestBody DraftCheckInRequest request) {
        checkInService.saveDraft(request);
        return ResponseEntity.ok("Draft saved");
    }

    /** Optional explicit cleanup (e.g. user clicks "Discard"). */
    @DeleteMapping("/draft")
    public ResponseEntity<String> discardDraft(@RequestParam String token, @RequestParam Integer studyWeek) {
        checkInService.discardDraft(token, studyWeek);
        return ResponseEntity.ok("Draft discarded");
    }
}
