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
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CheckInService {

    private static final Logger log = LoggerFactory.getLogger(CheckInService.class);

    // Mirror of frontend enum label → API value maps, used when promoting abandoned drafts
    private static final Map<String, String> CANN_METHOD = Map.of(
        "Juice / smoothie", "juice_smoothie",
        "Eaten directly",   "eaten_directly",
        "Cold infusion",    "cold_infusion",
        "Mixed",            "mixed",
        "None this week",   "none_this_week"
    );
    private static final Map<String, String> CANN_STRAIN = Map.of(
        "Sativa", "sativa", "Indica", "indica", "Balanced", "balanced", "Not selected", "not_selected"
    );
    private static final Map<String, String> EX_DAYS = Map.of(
        "Zero", "zero", "1–2 days", "1to2", "3–4 days", "3to4", "5+ days", "5plus"
    );
    private static final Map<String, String> EX_TYPE = Map.of(
        "Aerobic", "aerobic", "Resistance", "resistance", "Walking", "walking", "Mixed", "mixed", "None", "none"
    );
    private static final Map<String, String> MED_CHANGE = Map.of(
        "No changes", "no_changes", "Dose reduced", "dose_reduced",
        "Medication stopped", "medication_stopped", "New med added", "new_med_added"
    );
    private static final Map<String, String> STD_CARE = Map.of(
        "No", "no", "Scheduled visit", "yes_scheduled_visit",
        "Lab A1C", "yes_lab_a1c", "Other", "yes_other"
    );

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

    public Optional<CheckInEntity> getCheckIn(String token, Integer studyWeek) {
        return checkInRepository.findByTokenAndStudyWeek(token, studyWeek);
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

    @Transactional
    public void promoteAbandonedDrafts(List<DraftCheckInEntity> drafts) {
        for (DraftCheckInEntity draft : drafts) {
            try {
                if (checkInRepository.existsByTokenAndStudyWeek(draft.getToken(), draft.getStudyWeek())) {
                    draftCheckInRepository.delete(draft);
                    continue;
                }

                Map<String, Object> d = draft.getDraftData();
                CheckInEntity c = new CheckInEntity();
                c.setToken(draft.getToken());
                c.setStudyWeek(draft.getStudyWeek());
                c.setSubmittedAtDayOffset(14);

                List<?> hemp = listOrNull(d.get("hemp"));
                if (hemp != null && hemp.size() >= 7) {
                    c.setHempDayMon(boolOrNull(hemp.get(0)));
                    c.setHempDayTue(boolOrNull(hemp.get(1)));
                    c.setHempDayWed(boolOrNull(hemp.get(2)));
                    c.setHempDayThu(boolOrNull(hemp.get(3)));
                    c.setHempDayFri(boolOrNull(hemp.get(4)));
                    c.setHempDaySat(boolOrNull(hemp.get(5)));
                    c.setHempDaySun(boolOrNull(hemp.get(6)));
                }
                c.setHempAmountG(decOrNull(d.get("hempAmt")));

                List<?> cannabis = listOrNull(d.get("cannabis"));
                if (cannabis != null && cannabis.size() >= 7) {
                    c.setCannabisDayMon(boolOrNull(cannabis.get(0)));
                    c.setCannabisDayTue(boolOrNull(cannabis.get(1)));
                    c.setCannabisDayWed(boolOrNull(cannabis.get(2)));
                    c.setCannabisDayThu(boolOrNull(cannabis.get(3)));
                    c.setCannabisDayFri(boolOrNull(cannabis.get(4)));
                    c.setCannabisDaySat(boolOrNull(cannabis.get(5)));
                    c.setCannabisDaySun(boolOrNull(cannabis.get(6)));
                }
                c.setCannabisAmountG(decOrNull(d.get("cannAmt")));
                c.setCannabisMethod(CANN_METHOD.get(strOrNull(d.get("cannMethod"))));
                c.setCannabisStrainType(CANN_STRAIN.get(strOrNull(d.get("cannStrain"))));

                c.setGlucoseUnit(strOrNull(d.get("glucoseUnit")));
                List<?> gDays = listOrNull(d.get("glucoseDays"));
                if (gDays != null && gDays.size() >= 7) {
                    c.setGlucoseMon(decOrNull(gDays.get(0)));
                    c.setGlucoseTue(decOrNull(gDays.get(1)));
                    c.setGlucoseWed(decOrNull(gDays.get(2)));
                    c.setGlucoseThu(decOrNull(gDays.get(3)));
                    c.setGlucoseFri(decOrNull(gDays.get(4)));
                    c.setGlucoseSat(decOrNull(gDays.get(5)));
                    c.setGlucoseSun(decOrNull(gDays.get(6)));
                }

                c.setCgmTirPct(decOrNull(d.get("cgmTir")));
                c.setCgmTarPct(decOrNull(d.get("cgmTar")));
                c.setCgmTbrPct(decOrNull(d.get("cgmTbr")));
                c.setCgmCvPct(decOrNull(d.get("cgmCv")));

                c.setExerciseDays(EX_DAYS.get(strOrNull(d.get("exDays"))));
                c.setExerciseTypeThisWeek(EX_TYPE.get(strOrNull(d.get("exType"))));

                if (d.get("wb") instanceof Map<?, ?> wb) {
                    c.setWbEnergy(intOrNull(wb.get("energy")));
                    c.setWbMood(intOrNull(wb.get("mood")));
                    c.setWbDigestion(intOrNull(wb.get("digestion")));
                    c.setWbSleep(intOrNull(wb.get("sleep")));
                    c.setWbHydration(intOrNull(wb.get("hydration")));
                    c.setWbPain(intOrNull(wb.get("comfort")));
                }

                c.setWeightValue(decOrNull(d.get("weight")));
                String rawUnit = strOrNull(d.get("unit"));
                c.setWeightUnit("lb".equals(rawUnit) ? "lbs" : rawUnit);

                c.setMedicationChange(MED_CHANGE.get(strOrNull(d.get("medChange"))));
                c.setStandardCareContact(STD_CARE.get(strOrNull(d.get("stdCare"))));
                c.setFreeTextNote(strOrNull(d.get("note")));

                int hempCount = countTrue(c.getHempDayMon(), c.getHempDayTue(), c.getHempDayWed(),
                        c.getHempDayThu(), c.getHempDayFri(), c.getHempDaySat(), c.getHempDaySun());
                int cannCount = countTrue(c.getCannabisDayMon(), c.getCannabisDayTue(), c.getCannabisDayWed(),
                        c.getCannabisDayThu(), c.getCannabisDayFri(), c.getCannabisDaySat(), c.getCannabisDaySun());
                BigDecimal score = BigDecimal.valueOf(hempCount + cannCount)
                        .divide(BigDecimal.valueOf(14), 3, RoundingMode.HALF_UP);
                c.setHempDaysCount(hempCount);
                c.setCannabisDaysCount(cannCount);
                c.setCombinedComplianceScore(score);
                c.setWeekCompliant(score.compareTo(COMPLIANT_THRESHOLD) >= 0);

                checkInRepository.save(c);
                draftCheckInRepository.delete(draft);
                auditService.logEvent("checkin_auto_promoted", draft.getToken(), draft.getStudyWeek(), 14, false);
                log.info("Auto-promoted draft to check-in: token={} week={}", draft.getToken(), draft.getStudyWeek());

            } catch (Exception e) {
                log.error("Failed to promote draft token={} week={}: {}", draft.getToken(), draft.getStudyWeek(), e.getMessage());
            }
        }
    }

    private static String strOrNull(Object o) {
        return o instanceof String s && !s.isBlank() ? s : null;
    }

    private static Boolean boolOrNull(Object o) {
        return o instanceof Boolean b ? b : null;
    }

    private static BigDecimal decOrNull(Object o) {
        if (o == null) return null;
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }

    private static Integer intOrNull(Object o) {
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return null;
    }

    private static List<?> listOrNull(Object o) {
        return o instanceof List<?> l ? l : null;
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
