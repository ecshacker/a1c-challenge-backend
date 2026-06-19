package com.a1cchallenge.repository;

import com.a1cchallenge.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    // Useful for the "Dark Admin View" or bot-mitigation review
    List<AuditLogEntity> findByAnomalyFlagTrue();

    // Useful for investigating a specific hashed token's activity pattern
    List<AuditLogEntity> findByTokenHashOrderByEventYearWeekDesc(String tokenHash);
}
