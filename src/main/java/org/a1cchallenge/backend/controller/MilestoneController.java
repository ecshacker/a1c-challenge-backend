package com.a1cchallenge.controller;

import com.a1cchallenge.dto.MilestoneRequest;
import com.a1cchallenge.dto.MilestoneResponse;
import com.a1cchallenge.service.MilestoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @PostMapping
    public ResponseEntity<MilestoneResponse> submitMilestone(@Valid @RequestBody MilestoneRequest request) {
        MilestoneResponse response = milestoneService.processMilestone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
