package org.a1cchallenge.backend.service;

import org.a1cchallenge.backend.entity.AuditLogEntity;
import org.a1cchallenge.backend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;

/**
 * Privacy-first audit logging. The raw token is hashed with SHA-256 before it
 * ever touches the database or a log line. Only coarse temporal markers
 * (ISO year-week, study_week, day offset) are recorded - never wall-clock
 * timestamps tied to a token, never an IP.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persist a hashed audit event. Runs in its own transaction and never
     * propagates a failure back into the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String eventType, String rawToken, Integer studyWeek, Integer dayOffset, boolean anomalyFlag) {
        try {
            String tokenHash = sha256(rawToken);

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            WeekFields isoWeeks = WeekFields.ISO;
            int weekBasedYear = now.get(isoWeeks.weekBasedYear());
            int weekNumber = now.get(isoWeeks.weekOfWeekBasedYear());
            String yearWeek = String.format("%04d-%02d", weekBasedYear, weekNumber);

            // Anomaly detection: impossible submission speed for a hashed token.
            boolean flagged = anomalyFlag || detectRapidSequence(tokenHash, eventType);

            AuditLogEntity entry = new AuditLogEntity(
                    eventType, tokenHash, studyWeek, dayOffset, yearWeek, flagged);
            auditLogRepository.save(entry);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        } catch (Exception e) {
            // Swallow - never break the participant-facing transaction over an audit write.
            log.warn("Audit log write failed for event {}: {}", eventType, e.getMessage());
        }
    }

    /**
     * Flags suspicious sequences: a checkin_submit and a milestone_submit for the
     * same hashed token within the same ISO week is treated as a soft anomaly for
     * later manual review (Section: Bot Mitigation).
     */
    private boolean detectRapidSequence(String tokenHash, String eventType) {
        if (!"milestone_submit".equals(eventType)) {
            return false;
        }
        return auditLogRepository.findByTokenHashOrderByEventYearWeekDesc(tokenHash).stream()
                .anyMatch(e -> "checkin_submit".equals(e.getEventType()));
    }

    public String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
