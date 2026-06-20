package org.a1cchallenge.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Transient "fill-as-you-go" draft state. Deleted on successful check-in submit
 * and aged out by the scheduled purge job (Section 5).
 */
@Entity
@Table(name = "draft_checkins")
@Getter
@Setter
public class DraftCheckInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "draft_id")
    private UUID draftId;

    @Column(name = "token", nullable = false, length = 14)
    private String token;

    @Column(name = "study_week", nullable = false)
    private Integer studyWeek;

    @Column(name = "last_saved_offset", nullable = false)
    private Integer lastSavedOffset;

    // Maps to PostgreSQL JSONB. Built into Hibernate 6 (Spring Boot 3.x).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> draftData;
}
