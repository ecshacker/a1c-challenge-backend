package com.a1cchallenge.validation;

import com.a1cchallenge.dto.EnrollmentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Class-level cross-field validation mirroring the Section 7.1 DB CHECK
 * constraints, surfaced as clean 400 responses instead of raw constraint
 * violations from PostgreSQL.
 */
public class EnrollmentCrossFieldValidator implements ConstraintValidator<ValidEnrollment, EnrollmentRequest> {

    @Override
    public boolean isValid(EnrollmentRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;
        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Rule 1: Tribal Nation (7.1.5) - required iff ethnicity is AIAN
        if ("american_indian_alaska_native".equalsIgnoreCase(request.getEthnicity())) {
            if (isBlank(request.getTribalNation())) {
                isValid = violation(context, "Tribal nation is required when ethnicity is AIAN", "tribalNation");
            }
        } else if (!isBlank(request.getTribalNation())) {
            isValid = violation(context, "Tribal nation can only be provided if ethnicity is AIAN", "tribalNation");
        }

        // Rule 2: CGM Device (7.1.6) - only if glucose_monitoring_type = 'cgm'
        if (!"cgm".equalsIgnoreCase(request.getGlucoseMonitoringType()) && !isBlank(request.getCgmDevice())) {
            isValid = violation(context, "CGM device can only be provided if glucose_monitoring_type is 'cgm'", "cgmDevice");
        }

        // Rule 3: Medication mutual exclusivity (7.1.3)
        boolean hasOtherMeds = Boolean.TRUE.equals(request.getMedMetformin())
                || Boolean.TRUE.equals(request.getMedGlp1())
                || Boolean.TRUE.equals(request.getMedSglt2())
                || Boolean.TRUE.equals(request.getMedInsulin())
                || Boolean.TRUE.equals(request.getMedSulfonylurea());
        if (Boolean.TRUE.equals(request.getMedNone()) && hasOtherMeds) {
            isValid = violation(context, "medNone cannot be true if other medications are selected", "medNone");
        }

        // Rule 4: Condition mutual exclusivity (mirrors chk_cond_none_exclusive)
        boolean hasOtherConds = Boolean.TRUE.equals(request.getCondNafld())
                || Boolean.TRUE.equals(request.getCondPcos())
                || Boolean.TRUE.equals(request.getCondHypertension())
                || Boolean.TRUE.equals(request.getCondHypothyroid())
                || Boolean.TRUE.equals(request.getCondIbdCrohns())
                || Boolean.TRUE.equals(request.getCondIbs())
                || Boolean.TRUE.equals(request.getCondFibromyalgia())
                || Boolean.TRUE.equals(request.getCondAnxietyDepression())
                || Boolean.TRUE.equals(request.getCondSleepDisorder())
                || Boolean.TRUE.equals(request.getCondDyslipidemia())
                || Boolean.TRUE.equals(request.getCondChronicPain())
                || Boolean.TRUE.equals(request.getCondPancreatitisHistory());
        if (Boolean.TRUE.equals(request.getCondNone()) && hasOtherConds) {
            isValid = violation(context, "condNone cannot be true if other conditions are selected", "condNone");
        }

        // Rule 5: Height/weight pairing + units (mirrors chk_height_weight_*)
        boolean hasW = request.getWeightValue() != null;
        boolean hasH = request.getHeightValue() != null;
        if (hasW != hasH) {
            isValid = violation(context, "Height and weight must be given together, or both left blank", "weightValue");
        }
        if (hasW && isBlank(request.getWeightUnit())) {
            isValid = violation(context, "Weight requires a unit", "weightUnit");
        }
        if (hasH && isBlank(request.getHeightUnit())) {
            isValid = violation(context, "Height requires a unit", "heightUnit");
        }

        // Rule 6: Waist value/unit pairing (mirrors chk_waist_pairing)
        boolean hasWaistV = request.getWaistCircumferenceValue() != null;
        boolean hasWaistU = !isBlank(request.getWaistCircumferenceUnit());
        if (hasWaistV != hasWaistU) {
            isValid = violation(context, "Waist circumference value and unit must be given together", "waistCircumferenceValue");
        }

        // Rule 7: Fructosamine value requires test type (mirrors chk_fructosamine_test_type_req)
        if (request.getBaselineFructosamine() != null && isBlank(request.getBaselineFructosamineTestType())) {
            isValid = violation(context, "Fructosamine test type is required when a value is given", "baselineFructosamineTestType");
        }

        return isValid;
    }

    private boolean violation(ConstraintValidatorContext ctx, String message, String node) {
        ctx.buildConstraintViolationWithTemplate(message).addPropertyNode(node).addConstraintViolation();
        return false;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
