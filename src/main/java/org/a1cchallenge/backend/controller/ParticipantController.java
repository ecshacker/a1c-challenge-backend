package com.a1cchallenge.controller;

import com.a1cchallenge.dto.*;
import com.a1cchallenge.service.ParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Enrollment & self-service endpoints. No session, no cookie, no @ModelAttribute:
 * the X-Participant-Token header is the only key.
 */
@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping("/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = participantService.enrollParticipant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ParticipantSelfResponse> getMe(
            @RequestHeader("X-Participant-Token") String token) {
        return ResponseEntity.ok(participantService.getSelf(token));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateSelf(
            @RequestHeader("X-Participant-Token") String token,
            @RequestBody ParticipantUpdateRequest request) {
        if (request.getStartDate() != null) {
            participantService.setStart(token, request.getStartDate());
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/baseline")
    public ResponseEntity<Void> updateBaseline(
            @RequestHeader("X-Participant-Token") String token,
            @Valid @RequestBody BaselineUpdateRequest request) {
        participantService.updateBaseline(token, request);
        return ResponseEntity.noContent().build();
    }
}
