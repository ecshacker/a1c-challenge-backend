package org.a1cchallenge.backend.scheduled;

import org.a1cchallenge.backend.entity.StudyConfig;
import org.a1cchallenge.backend.repository.DraftCheckInRepository;
import org.a1cchallenge.backend.service.StudyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Section 5 retention: abandoned drafts are purged 14 days past their week end.
 *
 * The DB stores only study_week (not wall-clock dates), so the current global
 * study week is derived from study_config.launch_date. A draft for study_week W
 * "ends" at the close of week W; allowing the 14-day (2-week) aging window, any
 * draft with W <= (currentGlobalWeek - 2) is safe to delete.
 */
@Component
public class DraftCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DraftCleanupJob.class);

    private final DraftCheckInRepository draftRepository;
    private final StudyConfigService studyConfigService;

    public DraftCleanupJob(DraftCheckInRepository draftRepository, StudyConfigService studyConfigService) {
        this.draftRepository = draftRepository;
        this.studyConfigService = studyConfigService;
    }

    @Scheduled(cron = "0 0 2 * * ?") // daily at 02:00
    @Transactional
    public void purgeAbandonedDrafts() {
        StudyConfig config = studyConfigService.getConfig();
        if (config == null || config.getLaunchDate() == null) {
            log.info("Draft purge skipped: study launch date not yet anchored.");
            return;
        }

        long daysSinceLaunch = ChronoUnit.DAYS.between(config.getLaunchDate(), Instant.now());
        int currentGlobalWeek = (int) (daysSinceLaunch / 7) + 1;
        int purgeBeforeWeek = currentGlobalWeek - 2; // 14-day aging window

        if (purgeBeforeWeek < 1) {
            log.info("Draft purge skipped: study too young (global week {}).", currentGlobalWeek);
            return;
        }

        int deleted = draftRepository.purgeDraftsBeforeWeek(purgeBeforeWeek);
        log.info("Draft purge complete: removed {} abandoned drafts at or before study week {}.",
                deleted, purgeBeforeWeek);
    }
}
