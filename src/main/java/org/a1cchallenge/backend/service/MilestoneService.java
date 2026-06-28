package org.a1cchallenge.backend.service;

import org.a1cchallenge.backend.dto.MilestoneRequest;
import org.a1cchallenge.backend.dto.MilestoneResponse;
import org.a1cchallenge.backend.entity.CheckInEntity;
import org.a1cchallenge.backend.entity.MilestoneEntity;
import org.a1cchallenge.backend.entity.ParticipantEntity;
import org.a1cchallenge.backend.exception.TokenLostException;
import org.a1cchallenge.backend.repository.CheckInRepository;
import org.a1cchallenge.backend.repository.MilestoneRepository;
import org.a1cchallenge.backend.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class MilestoneService {

    private static final BigDecimal CLINICAL_THRESHOLD = new BigDecimal("-0.5");

    private final MilestoneRepository milestoneRepository;
    private final CheckInRepository checkInRepository;
    private final ParticipantRepository participantRepository;
    private final AuditService auditService;
    private final AdherenceTierService adherenceTierService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            CheckInRepository checkInRepository,
                            ParticipantRepository participantRepository,
                            AuditService auditService,
                            AdherenceTierService adherenceTierService) {
        this.milestoneRepository = milestoneRepository;
        this.checkInRepository = checkInRepository;
        this.participantRepository = participantRepository;
        this.auditService = auditService;
        this.adherenceTierService = adherenceTierService;
    }

    @Transactional
    public MilestoneResponse processMilestone(MilestoneRequest request) {
        ParticipantEntity participant = participantRepository.findById(request.getToken())
                .orElseThrow(() -> new TokenLostException("Invalid participant token"));

        if (milestoneRepository.existsByTokenAndStudyWeek(request.getToken(), request.getStudyWeek())) {
            throw new IllegalStateException("Milestone for this study week has already been submitted.");
        }

        if ("four_week".equals(request.getMilestoneType()) && request.getStudyWeek() != 4) {
            throw new IllegalArgumentException("A 'four_week' milestone must be submitted exactly at study week 4.");
        }
        if ("eight_week".equals(request.getMilestoneType()) && request.getStudyWeek() != 8) {
            throw new IllegalArgumentException("An 'eight_week' milestone must be submitted exactly at study week 8.");
        }

        List<CheckInEntity> historical = checkInRepository
                .findByTokenAndStudyWeekLessThanEqual(request.getToken(), request.getStudyWeek());

        int checkinsSubmitted = historical.size();
        if (checkinsSubmitted == 0) {
            throw new IllegalStateException("Cannot submit a milestone without prior weekly check-ins.");
        }
        if ("four_week".equals(request.getMilestoneType()) && checkinsSubmitted < 4) {
            throw new IllegalStateException("A 'four_week' milestone requires at least 4 submitted check-ins.");
        }

        long checkinsCompliant = historical.stream().filter(CheckInEntity::isWeekCompliant).count();

        String adherenceTier = adherenceTierService.computeAdherenceTier(request.getToken(), request.getStudyWeek());

        BigDecimal cecdBaseline = computeCecdComposite(
                participant.getWbBaselineEnergy(), participant.getWbBaselineMood(),
                participant.getWbBaselineDigestion(), participant.getWbBaselineSleep(),
                participant.getWbBaselineHydration(), participant.getWbBaselinePain());

        BigDecimal cecdMilestone = computeCecdComposite(
                request.getWbEnergy(), request.getWbMood(), request.getWbDigestion(),
                request.getWbSleep(), request.getWbHydration(), request.getWbPain());

        BigDecimal deltaA1c = request.getMilestoneA1c().subtract(participant.getBaselineA1c());
        boolean deltaA1cClinicallyMeaningful = deltaA1c.compareTo(CLINICAL_THRESHOLD) <= 0;

        BigDecimal deltaFructosamine = null;
        if (request.getMilestoneFructosamine() != null && participant.getBaselineFructosamine() != null) {
            deltaFructosamine = request.getMilestoneFructosamine().subtract(participant.getBaselineFructosamine());
        }

        BigDecimal deltaCecd = (cecdMilestone != null && cecdBaseline != null)
                ? cecdMilestone.subtract(cecdBaseline)
                : null;

        MilestoneEntity m = new MilestoneEntity();
        m.setToken(request.getToken());
        m.setStudyWeek(request.getStudyWeek());
        m.setMilestoneType(request.getMilestoneType());
        m.setMilestoneA1c(request.getMilestoneA1c());
        m.setMilestoneA1cTestType(request.getMilestoneA1cTestType());
        m.setMilestoneFructosamine(request.getMilestoneFructosamine());
        m.setMilestoneFructosamineTestType(request.getMilestoneFructosamineTestType());
        m.setWbEnergy(request.getWbEnergy());
        m.setWbMood(request.getWbMood());
        m.setWbDigestion(request.getWbDigestion());
        m.setWbSleep(request.getWbSleep());
        m.setWbHydration(request.getWbHydration());
        m.setWbPain(request.getWbPain());
        m.setSelfReportedAdherence(request.getSelfReportedAdherence());
        m.setMedicationChangeOverall(request.getMedicationChangeOverall());
        m.setWhatNext(request.getWhatNext());
        m.setFreeTextNote(request.getFreeTextNote());

        m.setCheckinsSubmitted(checkinsSubmitted);
        m.setCheckinsCompliant((int) checkinsCompliant);
        m.setAdherenceTier(adherenceTier);
        m.setCecdCompositeBaseline(cecdBaseline);
        m.setCecdCompositeMilestone(cecdMilestone);
        m.setDeltaA1c(deltaA1c);
        m.setDeltaA1cClinicallyMeaningful(deltaA1cClinicallyMeaningful);
        m.setDeltaFructosamine(deltaFructosamine);
        m.setDeltaCecdComposite(deltaCecd);

        milestoneRepository.save(m);
        auditService.logEvent("milestone_submit", request.getToken(), request.getStudyWeek(), 1, false);

        return new MilestoneResponse("Milestone recorded successfully",
                request.getStudyWeek(), deltaA1c, deltaA1cClinicallyMeaningful, adherenceTier);
    }

    public List<Integer> getSubmittedMilestoneWeeks(String token) {
        return milestoneRepository.findStudyWeeksByToken(token);
    }

    /** Section 8.1: mean of all six wellbeing dimensions; null if any is missing. */
    private BigDecimal computeCecdComposite(Integer e, Integer m, Integer d, Integer s, Integer h, Integer p) {
        if (e == null || m == null || d == null || s == null || h == null || p == null) {
            return null;
        }
        BigDecimal sum = BigDecimal.valueOf((long) e + m + d + s + h + p);
        return sum.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
    }
}
