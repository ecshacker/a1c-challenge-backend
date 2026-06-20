package org.a1cchallenge.backend.entity;

/** Global study lifecycle state controlling the enrollment kill switch. */
public enum StudyStatus {
    PRE_LAUNCH,
    OPEN,
    PAUSED_INVESTIGATION,
    CONCLUDED
}
