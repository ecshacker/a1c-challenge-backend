package com.a1cchallenge.service;

import com.a1cchallenge.entity.StudyConfig;
import com.a1cchallenge.entity.StudyStatus;
import com.a1cchallenge.repository.StudyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StudyConfigService {

    private final StudyConfigRepository studyConfigRepository;

    public StudyConfigService(StudyConfigRepository studyConfigRepository) {
        this.studyConfigRepository = studyConfigRepository;
    }

    public StudyStatus getCurrentStatus() {
        return studyConfigRepository.findById((short) 1)
                .map(StudyConfig::getStatus)
                .orElse(StudyStatus.PRE_LAUNCH); // safe fallback: stay locked
    }

    public StudyConfig getConfig() {
        return studyConfigRepository.findById((short) 1).orElse(null);
    }

    @Transactional
    public StudyConfig updateStatus(StudyStatus newStatus, String updatedBy) {
        StudyConfig config = studyConfigRepository.findById((short) 1).orElse(new StudyConfig());
        config.setId((short) 1);
        config.setStatus(newStatus);
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(updatedBy);
        // When moving to OPEN for the first time, anchor the global launch date.
        if (newStatus == StudyStatus.OPEN && config.getLaunchDate() == null) {
            config.setLaunchDate(Instant.now());
        }
        return studyConfigRepository.save(config);
    }
}
