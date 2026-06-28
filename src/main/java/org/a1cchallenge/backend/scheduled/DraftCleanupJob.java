package org.a1cchallenge.backend.scheduled;

import org.a1cchallenge.backend.entity.DraftCheckInEntity;
import org.a1cchallenge.backend.entity.StudyConfig;
import org.a1cchallenge.backend.repository.DraftCheckInRepository;
import org.a1cchallenge.backend.service.CheckInService;
import org.a1cchallenge.backend.service.StudyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Section 5 retention: abandoned drafts are auto-promoted to submitted check-ins
 * at the 14-day mark rather than deleted, so participant data is preserved.
 *
 * A draft for study_week W is eligible once W <= (currentGlobalWeek - 2), i.e.,
 * the 14-day window has closed.
 */
@Component
public class DraftCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DraftCleanupJob.class);

    private final DraftCheckInRepository draftRepository;
    private final CheckInService checkInService;
    private final StudyConfigService studyConfigService;

    public DraftCleanupJob(DraftCheckInRepository draftRepository,
                           CheckInService checkInService,
                           StudyConfigService studyConfigService) {
        this.draftRepository = draftRepository;
        this.checkInService = checkInService;
        this.studyConfigService = studyConfigService;
    }

    @Scheduled(cron = "0 0 2 * * ?") // daily at 02:00
    public void promoteAbandonedDrafts() {
        StudyConfig config = studyConfigService.getConfig();
        if (config == null || config.getLaunchDate() == null) {
            log.info("Draft promotion skipped: study launch date not yet anchored.");
            return;
        }

        long daysSinceLaunch = ChronoUnit.DAYS.between(config.getLaunchDate(), Instant.now());
        int currentGlobalWeek = (int) (daysSinceLaunch / 7) + 1;
        int promoteBeforeWeek = currentGlobalWeek - 2; // 14-day window closed

        if (promoteBeforeWeek < 1) {
            log.info("Draft promotion skipped: study too young (global week {}).", currentGlobalWeek);
            return;
        }

        List<DraftCheckInEntity> candidates = draftRepository.findAllByStudyWeekLessThanEqual(promoteBeforeWeek);
        if (candidates.isEmpty()) {
            log.info("Draft promotion: no eligible drafts at or before week {}.", promoteBeforeWeek);
            return;
        }

        log.info("Draft promotion: promoting {} draft(s) at or before week {}.", candidates.size(), promoteBeforeWeek);
        checkInService.promoteAbandonedDrafts(candidates);
    }
}
