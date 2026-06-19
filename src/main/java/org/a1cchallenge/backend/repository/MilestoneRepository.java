package com.a1cchallenge.repository;

import com.a1cchallenge.entity.MilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<MilestoneEntity, UUID> {

    // Section 7.3.4: only one milestone row per token per study_week
    boolean existsByTokenAndStudyWeek(String token, Integer studyWeek);

    boolean existsByTokenAndStudyWeekAndMilestoneA1cIsNotNull(String token, Integer studyWeek);
}
