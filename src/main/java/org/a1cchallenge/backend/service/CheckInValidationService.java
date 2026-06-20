package org.a1cchallenge.backend.service;

import org.a1cchallenge.backend.dto.CheckInRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Section 7.2 "flag but do not reject" soft validation. Out-of-range glucose
 * and TIR+TAR+TBR sums produce warnings; the data is still saved as entered.
 */
@Service
public class CheckInValidationService {

    private static final BigDecimal MGDL_LOW = new BigDecimal("40");
    private static final BigDecimal MGDL_HIGH = new BigDecimal("600");
    private static final BigDecimal MMOLL_LOW = new BigDecimal("2.2");
    private static final BigDecimal MMOLL_HIGH = new BigDecimal("33.3");
    private static final BigDecimal SUM_LOW = new BigDecimal("95.0");
    private static final BigDecimal SUM_HIGH = new BigDecimal("105.0");

    public List<String> validateSoftRules(CheckInRequest request) {
        List<String> warnings = new ArrayList<>();

        BigDecimal[] readings = {
                request.getGlucoseMon(), request.getGlucoseTue(), request.getGlucoseWed(),
                request.getGlucoseThu(), request.getGlucoseFri(), request.getGlucoseSat(), request.getGlucoseSun()
        };

        for (BigDecimal reading : readings) {
            if (reading == null) continue;
            if ("mgdl".equalsIgnoreCase(request.getGlucoseUnit())) {
                if (reading.compareTo(MGDL_LOW) < 0 || reading.compareTo(MGDL_HIGH) > 0) {
                    warnings.add("Warning: Glucose reading " + reading
                            + " mg/dL is outside the plausible range (40-600). Recorded as entered.");
                }
            } else if ("mmoll".equalsIgnoreCase(request.getGlucoseUnit())) {
                if (reading.compareTo(MMOLL_LOW) < 0 || reading.compareTo(MMOLL_HIGH) > 0) {
                    warnings.add("Warning: Glucose reading " + reading
                            + " mmol/L is outside the plausible range (2.2-33.3). Recorded as entered.");
                }
            }
        }

        if (request.getCgmTirPct() != null && request.getCgmTarPct() != null && request.getCgmTbrPct() != null) {
            BigDecimal sum = request.getCgmTirPct().add(request.getCgmTarPct()).add(request.getCgmTbrPct());
            if (sum.compareTo(SUM_LOW) < 0 || sum.compareTo(SUM_HIGH) > 0) {
                warnings.add("Warning: TIR + TAR + TBR sum is " + sum
                        + "%. Expected ~100% (95-105% tolerance). Recorded as entered.");
            }
        }

        return warnings;
    }
}
