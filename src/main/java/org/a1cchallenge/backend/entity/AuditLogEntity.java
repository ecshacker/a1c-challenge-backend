package com.a1cchallenge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Privacy-first audit record. Stores only a SHA-256 hash of the token plus
 * coarse, non-wall-clock temporal markers. The raw token is never persisted.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "study_week")
    private Integer studyWeek;

    @Column(name = "event_week_offset", nullable = false)
    private Integer eventWeekOffset;

    @Column(name = "event_year_week", nullable = false, length = 8)
    private String eventYearWeek;

    @Column(name = "anomaly_flag", nullable = false)
    private Boolean anomalyFlag = false;

    public AuditLogEntity() {}

    public AuditLogEntity(String eventType, String tokenHash, Integer studyWeek,
                          Integer eventWeekOffset, String eventYearWeek, Boolean anomalyFlag) {
        this.eventType = eventType;
        this.tokenHash = tokenHash;
        this.studyWeek = studyWeek;
        this.eventWeekOffset = eventWeekOffset;
        this.eventYearWeek = eventYearWeek;
        this.anomalyFlag = anomalyFlag != null ? anomalyFlag : false;
    }
}
