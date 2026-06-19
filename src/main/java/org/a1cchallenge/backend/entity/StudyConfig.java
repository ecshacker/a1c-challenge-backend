package com.a1cchallenge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Single-row (id = 1) global study configuration / kill switch. */
@Entity
@Table(name = "study_config")
@Getter
@Setter
public class StudyConfig {

    @Id
    @Column(name = "id")
    private Short id = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudyStatus status;

    @Column(name = "launch_date")
    private Instant launchDate;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public StudyConfig() {}
}
