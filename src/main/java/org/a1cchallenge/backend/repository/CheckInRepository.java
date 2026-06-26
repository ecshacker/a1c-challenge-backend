package org.a1cchallenge.backend.repository;

import org.a1cchallenge.backend.entity.CheckInEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CheckInRepository extends JpaRepository<CheckInEntity, UUID> {

    // Enforces Section 7.2.2: only one checkin row per token per study_week
    boolean existsByTokenAndStudyWeek(String token, Integer studyWeek);

    // Fetch all check-ins up to the milestone week for tier calculation
    List<CheckInEntity> findByTokenAndStudyWeekLessThanEqual(String token, Integer studyWeek);

    // Returns the list of study week numbers already submitted by this participant
    @Query("SELECT c.studyWeek FROM CheckInEntity c WHERE c.token = :token")
    List<Integer> findStudyWeeksByToken(@Param("token") String token);
}
