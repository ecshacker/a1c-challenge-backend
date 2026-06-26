package org.a1cchallenge.backend.service;

import org.a1cchallenge.backend.dto.CheckInRequest;
import org.a1cchallenge.backend.dto.CheckInResponse;
import org.a1cchallenge.backend.dto.DraftCheckInRequest;
import org.a1cchallenge.backend.entity.CheckInEntity;
import org.a1cchallenge.backend.entity.DraftCheckInEntity;
import org.a1cchallenge.backend.entity.ParticipantEntity;
import org.a1cchallenge.backend.exception.TokenLostException;
import org.a1cchallenge.backend.repository.CheckInRepository;
import org.a1cchallenge.backend.repository.DraftCheckInRepository;
import org.a1cchallenge.backend.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CheckInService {

    private static final BigDecimal COMPLIANT_THRESHOLD = new BigDecimal("0.800");

    private final CheckInRepository checkInRepository;
    private final DraftCheckInRepository draftCheckInRepository;
    private final ParticipantRepository participantRepository;
    private final AuditService auditService;

    public CheckInService(CheckInRepository checkInRepository,
                          DraftCheckInRepository draftCheckInRepository,
                          ParticipantRepository participantRepository,
                          AuditService auditService) {
        this.checkInRepository = checkInRepository;
        this.draftCheckInRepository = draftCheckInRepository;
        this.participantRepository = participantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CheckInResponse processCheckIn(CheckInRequest request) {
        ParticipantEntity participant = participantRepository.findById(request.getToken())
                .orElseThrow(() -> new TokenLostException("Invalid participant token"));

        // Exposure-clock precondition
        if (participant.getStartDate() == null) {
            throw new IllegalStateException("Cannot submit a check-in before the start date is set.");
        }

        // Section 7.2.2: reject duplicate submission for the same week
        if (checkInRepository.existsByTokenAndStudyWeek(request.getToken(), request.getStudyWeek())) {
            throw new IllegalStateException("Check-in for this study week has already been submitted.");
        }

        // Section 7.2.4: silently ignore CGM fields if participant is not a CGM user
        if (!"cgm".equalsIgnoreCase(participant.getGlucoseMonitoringType())) {
            request.setCgmTirPct(null);
            request.setCgmTarPct(null);
            request.setCgmTbrPct(null);
            request.setCgmCvPct(null);
        }

        // Section 3.9: compute compliance server-side
        int hempDays = countTrue(request.getHempDayMon(), request.getHempDayTue(), request.getHempDayWed(),
                request.getHempDayThu(), request.getHempDayFri(), request.getHempDaySat(), request.getHempDaySun());
        int cannabisDays = countTrue(request.getCannabisDayMon(), request.getCannabisDayTue(), request.getCannabisDayWed(),
                request.getCannabisDayThu(), request.getCannabisDayFri(), request.getCannabisDaySat(), request.getCannabisDaySun());

        // (hemp_days_count + cannabis_days_count) / 14
        BigDecimal complianceScore = BigDecimal.valueOf(hempDays + cannabisDays)
                .divide(BigDecimal.valueOf(14), 3, RoundingMode.HALF_UP);
        boolean weekCompliant = complianceScore.compareTo(COMPLIANT_THRESHOLD) >= 0;

        CheckInEntity c = new CheckInEntity();
        mapCheckInFields(request, c);
        c.setHempDaysCount(hempDays);
        c.setCannabisDaysCount(cannabisDays);
        c.setCombinedComplianceScore(complianceScore);
        c.setWeekCompliant(weekCompliant);

        checkInRepository.save(c);

        // Section 5: atomically delete the draft on successful submit
        draftCheckInRepository.deleteByTokenAndStudyWeek(request.getToken(), request.getStudyWeek());

        auditService.logEvent("checkin_submit", request.getToken(),
                request.getStudyWeek(), request.getSubmittedAtDayOffset(), false);

        return new CheckInResponse("Check-in submitted successfully",
                request.getStudyWeek(), complianceScore, weekCompliant);
    }

    public List<Integer> getSubmittedWeeks(String token) {
        return checkInRepository.findStudyWeeksByToken(token);
    }

    public Optional<DraftCheckInEntity> getDraft(String token, Integer studyWeek) {
        return draftCheckInRepository.findByTokenAndStudyWeek(token, studyWeek);
    }

    @Transactional
    public void saveDraft(DraftCheckInRequest request) {
        participantRepository.findById(request.getToken())
                .orElseThrow(() -> new TokenLostException("Invalid participant token"));

        DraftCheckInEntity draft = draftCheckInRepository
                .findByTokenAndStudyWeek(request.getToken(), request.getStudyWeek())
                .orElse(new DraftCheckInEntity());

        draft.setToken(request.getToken());
        draft.setStudyWeek(request.getStudyWeek());
        draft.setLastSavedOffset(request.getLastSavedOffset());
        draft.setDraftData(request.getDraftData());

        draftCheckInRepository.save(draft);
        auditService.logEvent("draft_save", request.getToken(),
                request.getStudyWeek(), request.getLastSavedOffset(), false);
    }

    @Transactional
    public void discardDraft(String token, Integer studyWeek) {
        draftCheckInRepository.deleteByTokenAndStudyWeek(token, studyWeek);
    }

    private int countTrue(Boolean... values) {
        int count = 0;
        for (Boolean b : values) {
            if (Boolean.TRUE.equals(b)) count++; // null and false contribute 0
        }
        return count;
    }

    private void mapCheckInFields(CheckInRequest s, CheckInEntity c) {
        c.setToken(s.getToken());
        c.setStudyWeek(s.getStudyWeek());
        c.setSubmittedAtDayOffset(s.getSubmittedAtDayOffset());

        c.setHempDayMon(s.getHempDayMon());
        c.setHempDayTue(s.getHempDayTue());
        c.setHempDayWed(s.getHempDayWed());
        c.setHempDayThu(s.getHempDayThu());
        c.setHempDayFri(s.getHempDayFri());
        c.setHempDaySat(s.getHempDaySat());
        c.setHempDaySun(s.getHempDaySun());
        c.setHempAmountG(s.getHempAmountG());

        c.setCannabisDayMon(s.getCannabisDayMon());
        c.setCannabisDayTue(s.getCannabisDayTue());
        c.setCannabisDayWed(s.getCannabisDayWed());
        c.setCannabisDayThu(s.getCannabisDayThu());
        c.setCannabisDayFri(s.getCannabisDayFri());
        c.setCannabisDaySat(s.getCannabisDaySat());
        c.setCannabisDaySun(s.getCannabisDaySun());
        c.setCannabisMethod(s.getCannabisMethod());
        c.setCannabisAmountG(s.getCannabisAmountG());
        c.setCannabisStrainType(s.getCannabisStrainType());
        c.setCannabisThcaCbdaKnown(s.getCannabisThcaCbdaKnown());
        c.setCannabisProfileNotes(s.getCannabisProfileNotes());

        c.setGlucoseUnit(s.getGlucoseUnit());
        c.setGlucoseMon(s.getGlucoseMon());
        c.setGlucoseTue(s.getGlucoseTue());
        c.setGlucoseWed(s.getGlucoseWed());
        c.setGlucoseThu(s.getGlucoseThu());
        c.setGlucoseFri(s.getGlucoseFri());
        c.setGlucoseSat(s.getGlucoseSat());
        c.setGlucoseSun(s.getGlucoseSun());

        c.setCgmTirPct(s.getCgmTirPct());
        c.setCgmTarPct(s.getCgmTarPct());
        c.setCgmTbrPct(s.getCgmTbrPct());
        c.setCgmCvPct(s.getCgmCvPct());

        c.setExerciseDays(s.getExerciseDays());
        c.setExerciseTypeThisWeek(s.getExerciseTypeThisWeek());
        c.setWbEnergy(s.getWbEnergy());
        c.setWbMood(s.getWbMood());
        c.setWbDigestion(s.getWbDigestion());
        c.setWbSleep(s.getWbSleep());
        c.setWbHydration(s.getWbHydration());
        c.setWbPain(s.getWbPain());

        c.setWeightValue(s.getWeightValue());
        c.setWeightUnit(s.getWeightUnit());
        c.setMedicationChange(s.getMedicationChange());
        c.setStandardCareContact(s.getStandardCareContact());
        c.setFreeTextNote(s.getFreeTextNote());
    }
}
