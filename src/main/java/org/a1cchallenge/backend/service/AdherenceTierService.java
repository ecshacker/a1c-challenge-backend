package org.a1cchallenge.backend.service;

import org.a1cchallenge.backend.entity.CheckInEntity;
import org.a1cchallenge.backend.repository.CheckInRepository;
import org.a1cchallenge.backend.repository.MilestoneRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic adherence-tier routing (Analysis Plan Section 6). Generalized
 * for 4-week, 8-week, or any N-week evaluation. Tiers are objective and based
 * on stored server-side compliance scores, not self-report.
 */
@Service
public class AdherenceTierService {

    private static final BigDecimal T80 = new BigDecimal("0.800");
    private static final BigDecimal T90 = new BigDecimal("0.900");

    private final CheckInRepository checkInRepository;
    private final MilestoneRepository milestoneRepository;

    public AdherenceTierService(CheckInRepository checkInRepository, MilestoneRepository milestoneRepository) {
        this.checkInRepository = checkInRepository;
        this.milestoneRepository = milestoneRepository;
    }

    public String computeAdherenceTier(String token, int targetWeek) {
        List<CheckInEntity> checkins = checkInRepository.findByTokenAndStudyWeekLessThanEqual(token, targetWeek);
        int checkinsSubmitted = checkins.size();

        if (checkinsSubmitted == 0) {
            return "enrolled_only"; // attrition edge case; never persisted on a milestone row
        }

        long compliant80 = checkins.stream()
                .filter(c -> c.getCombinedComplianceScore().compareTo(T80) >= 0)
                .count();
        long compliant90 = checkins.stream()
                .filter(c -> c.getCombinedComplianceScore().compareTo(T90) >= 0)
                .count();

        boolean hasMilestoneA1c =
                milestoneRepository.existsByTokenAndStudyWeekAndMilestoneA1cIsNotNull(token, targetWeek);

        int requiredFor80 = (int) Math.floor(0.80 * targetWeek);
        int requiredFor50 = (int) Math.ceil(0.50 * targetWeek);

        int evaluatedSubmitted = Math.min(checkinsSubmitted, targetWeek);
        long evaluatedComp80 = Math.min(compliant80, targetWeek);
        long evaluatedComp90 = Math.min(compliant90, targetWeek);

        if (evaluatedSubmitted == targetWeek && evaluatedComp90 == targetWeek && hasMilestoneA1c) {
            return "high_adherence";
        }
        if (evaluatedSubmitted >= requiredFor80 && evaluatedComp80 >= requiredFor80 && hasMilestoneA1c) {
            return "per_protocol";
        }
        if (evaluatedSubmitted >= requiredFor50 && hasMilestoneA1c) {
            return "partial";
        }
        return "full_cohort";
    }
}
