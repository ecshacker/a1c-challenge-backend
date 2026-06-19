package com.a1cchallenge.service;

import com.a1cchallenge.dto.*;
import com.a1cchallenge.entity.ParticipantEntity;
import com.a1cchallenge.entity.StudyStatus;
import com.a1cchallenge.exception.StudyNotOpenException;
import com.a1cchallenge.exception.TokenLostException;
import com.a1cchallenge.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

@Service
public class ParticipantService {

    private final TokenGeneratorService tokenGeneratorService;
    private final AuditService auditService;
    private final ParticipantRepository participantRepository;
    private final StudyConfigService studyConfigService;

    public ParticipantService(TokenGeneratorService tokenGeneratorService,
                              AuditService auditService,
                              ParticipantRepository participantRepository,
                              StudyConfigService studyConfigService) {
        this.tokenGeneratorService = tokenGeneratorService;
        this.auditService = auditService;
        this.participantRepository = participantRepository;
        this.studyConfigService = studyConfigService;
    }

    @Transactional
    public EnrollmentResponse enrollParticipant(EnrollmentRequest request) {
        // 1. Kill switch / OSF gating
        if (studyConfigService.getCurrentStatus() != StudyStatus.OPEN) {
            throw new StudyNotOpenException(
                    "Enrollment is currently closed. Status: " + studyConfigService.getCurrentStatus());
        }

        // 2. Business-logic validation beyond the annotations
        validateA1cDate(request.getBaselineA1cMonth(), request.getBaselineA1cYear());

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        WeekFields iso = WeekFields.ISO;
        int enrollAtWeek = now.get(iso.weekOfWeekBasedYear());
        int weekBasedYear = now.get(iso.weekBasedYear());

        // 3. Generate secure token & map the full baseline record
        String newToken = tokenGeneratorService.generateToken();
        ParticipantEntity p = new ParticipantEntity();
        p.setToken(newToken);
        p.setEnrolledAtWeek(enrollAtWeek);
        p.setEnrolledAtYear(weekBasedYear);
        p.setSchemaVersion(1);
        // start_date intentionally left null: enrolled, not started.

        mapEnrollmentFields(request, p);
        participantRepository.save(p);

        // 4. Audit (hashed) & return token exactly once
        auditService.logEvent("enrollment", newToken, 1, 1, false);
        return new EnrollmentResponse(newToken,
                "Enrollment successful. Save this token securely; it cannot be recovered.");
    }

    @Transactional
    public void setStart(String token, LocalDate startDate) {
        ParticipantEntity participant = findOrThrow(token);

        if (startDate == null) {
            throw new IllegalArgumentException("start_date is required.");
        }
        if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("start_date must be a Monday.");
        }
        LocalDate today = LocalDate.now();
        // Lock: once the chosen Monday has arrived, the clock is running.
        if (participant.getStartDate() != null && !participant.getStartDate().isAfter(today)) {
            throw new IllegalStateException("Start date is locked; the exposure clock has already begun.");
        }
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (startDate.isBefore(thisMonday)) {
            throw new IllegalArgumentException("start_date cannot be in a past week.");
        }
        if (startDate.isAfter(thisMonday.plusWeeks(8))) {
            throw new IllegalArgumentException("start_date is too far in the future.");
        }

        participant.setStartDate(startDate);
        participantRepository.save(participant);
        auditService.logEvent("start_set", token, 1, 1, false);
    }

    @Transactional(readOnly = true)
    public ParticipantSelfResponse getSelf(String token) {
        ParticipantEntity p = findOrThrow(token);

        ParticipantSelfResponse r = new ParticipantSelfResponse();
        r.setStartDate(p.getStartDate());
        r.setBaselineA1c(p.getBaselineA1c());
        r.setBaselineA1cTestType(p.getBaselineA1cTestType());
        r.setBaselineA1cMonth(p.getBaselineA1cMonth());
        r.setBaselineA1cYear(p.getBaselineA1cYear());
        r.setBaselineFructosamine(p.getBaselineFructosamine());
        r.setBaselineFructosamineTestType(p.getBaselineFructosamineTestType());
        r.setWeightValue(p.getWeightValue());
        r.setWeightUnit(p.getWeightUnit());
        r.setHeightValue(p.getHeightValue());
        r.setHeightUnit(p.getHeightUnit());

        LocalDate start = p.getStartDate();
        LocalDate today = LocalDate.now();
        if (start == null) {
            r.setStudyWeek(null);
            r.setBaselineEditable(true);
        } else {
            long days = ChronoUnit.DAYS.between(start, today);
            r.setStudyWeek(days >= 0 ? (int) (days / 7) + 1 : null);
            r.setBaselineEditable(today.isBefore(start.plusWeeks(1)));
        }
        return r;
    }

    @Transactional
    public void updateBaseline(String token, BaselineUpdateRequest req) {
        ParticipantEntity p = findOrThrow(token);

        // Lock: editable until week-1 close. Once week 2 begins, frozen.
        if (p.getStartDate() != null && !LocalDate.now().isBefore(p.getStartDate().plusWeeks(1))) {
            throw new IllegalStateException("Baseline is locked; week 1 has closed.");
        }

        boolean hasW = req.getWeightValue() != null;
        boolean hasH = req.getHeightValue() != null;
        if (hasW != hasH) {
            throw new IllegalArgumentException("Weight and height must be given together, or both left blank.");
        }
        if ((hasW && req.getWeightUnit() == null) || (hasH && req.getHeightUnit() == null)) {
            throw new IllegalArgumentException("Weight and height each need a unit.");
        }
        if (req.getBaselineFructosamine() != null
                && (req.getBaselineFructosamineTestType() == null || req.getBaselineFructosamineTestType().isBlank())) {
            throw new IllegalArgumentException("Fructosamine test type is required when a value is given.");
        }

        p.setBaselineA1c(req.getBaselineA1c());
        if (req.getBaselineA1cTestType() != null && !req.getBaselineA1cTestType().isBlank()) {
            p.setBaselineA1cTestType(req.getBaselineA1cTestType());
        }
        p.setBaselineFructosamine(req.getBaselineFructosamine());
        p.setBaselineFructosamineTestType(req.getBaselineFructosamineTestType());
        p.setWeightValue(req.getWeightValue());
        p.setWeightUnit(hasW ? req.getWeightUnit() : null);
        p.setHeightValue(req.getHeightValue());
        p.setHeightUnit(hasH ? req.getHeightUnit() : null);

        participantRepository.save(p);
    }

    private ParticipantEntity findOrThrow(String token) {
        return participantRepository.findById(token)
                .orElseThrow(() -> new TokenLostException(
                        "Token not recognized. If you cleared your browser data, your token is permanently lost."));
    }

    private void validateA1cDate(Integer month, Integer year) {
        // Eligibility: the baseline A1C test should be reasonably recent. The
        // build-trail flags a 60-day target; without a test-day we enforce that
        // the month/year is not in the future and not absurdly old.
        if (month == null || year == null) {
            throw new IllegalArgumentException("Baseline A1C test month and year are required.");
        }
        LocalDate testMonthStart = LocalDate.of(year, month, 1);
        LocalDate today = LocalDate.now();
        if (testMonthStart.isAfter(today.withDayOfMonth(1))) {
            throw new IllegalArgumentException("Baseline A1C test date cannot be in the future.");
        }
        if (testMonthStart.isBefore(today.minusMonths(4).withDayOfMonth(1))) {
            throw new IllegalArgumentException("Baseline A1C test is too old to establish eligibility.");
        }
    }

    /** Maps every enrollment field from request to entity (deferred as TODO in the build-trail). */
    private void mapEnrollmentFields(EnrollmentRequest s, ParticipantEntity p) {
        p.setAgeRange(s.getAgeRange());
        p.setBiologicalSex(s.getBiologicalSex());
        p.setEthnicity(s.getEthnicity());
        p.setTribalNation(s.getTribalNation());
        p.setCountryRegion(s.getCountryRegion());

        p.setDiabetesType(s.getDiabetesType());
        p.setBaselineA1c(s.getBaselineA1c());
        p.setBaselineA1cTestType(s.getBaselineA1cTestType());
        p.setBaselineA1cMonth(s.getBaselineA1cMonth());
        p.setBaselineA1cYear(s.getBaselineA1cYear());
        p.setBaselineFructosamine(s.getBaselineFructosamine());
        p.setBaselineFructosamineTestType(s.getBaselineFructosamineTestType());
        p.setGlucoseMonitoringType(s.getGlucoseMonitoringType());
        p.setCgmDevice(s.getCgmDevice());

        p.setCareApproach(s.getCareApproach());
        p.setMedMetformin(s.getMedMetformin());
        p.setMedGlp1(s.getMedGlp1());
        p.setMedSglt2(s.getMedSglt2());
        p.setMedInsulin(s.getMedInsulin());
        p.setMedSulfonylurea(s.getMedSulfonylurea());
        p.setMedNone(s.getMedNone());

        p.setHempIntendedDailyG(s.getHempIntendedDailyG());
        p.setCarbIntakeBand(s.getCarbIntakeBand());
        p.setExerciseDaysPerWeek(s.getExerciseDaysPerWeek());
        p.setExerciseTypeAerobic(s.getExerciseTypeAerobic());
        p.setExerciseTypeResistance(s.getExerciseTypeResistance());
        p.setExerciseTypeWalking(s.getExerciseTypeWalking());
        p.setExerciseTypeYoga(s.getExerciseTypeYoga());
        p.setExerciseTypeMixed(s.getExerciseTypeMixed());
        p.setDietaryPattern(s.getDietaryPattern());

        p.setWeightValue(s.getWeightValue());
        p.setWeightUnit(s.getWeightUnit());
        p.setHeightValue(s.getHeightValue());
        p.setHeightUnit(s.getHeightUnit());
        p.setWaistCircumferenceValue(s.getWaistCircumferenceValue());
        p.setWaistCircumferenceUnit(s.getWaistCircumferenceUnit());

        p.setCondNafld(s.getCondNafld());
        p.setCondPcos(s.getCondPcos());
        p.setCondHypertension(s.getCondHypertension());
        p.setCondHypothyroid(s.getCondHypothyroid());
        p.setCondIbdCrohns(s.getCondIbdCrohns());
        p.setCondIbs(s.getCondIbs());
        p.setCondFibromyalgia(s.getCondFibromyalgia());
        p.setCondAnxietyDepression(s.getCondAnxietyDepression());
        p.setCondSleepDisorder(s.getCondSleepDisorder());
        p.setCondDyslipidemia(s.getCondDyslipidemia());
        p.setCondChronicPain(s.getCondChronicPain());
        p.setCondPancreatitisHistory(s.getCondPancreatitisHistory());
        p.setCondNone(s.getCondNone());

        p.setWbBaselineEnergy(s.getWbBaselineEnergy());
        p.setWbBaselineMood(s.getWbBaselineMood());
        p.setWbBaselineDigestion(s.getWbBaselineDigestion());
        p.setWbBaselineSleep(s.getWbBaselineSleep());
        p.setWbBaselineHydration(s.getWbBaselineHydration());
        p.setWbBaselinePain(s.getWbBaselinePain());
    }
}
