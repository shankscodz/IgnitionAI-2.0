package com.ignitionai.consistencychecks.checks;

import com.ignitionai.consistencychecks.schema.CheckDecision;
import com.ignitionai.consistencychecks.schema.CheckResult;
import com.ignitionai.factextraction.schema.CandidateFact;
import com.ignitionai.factextraction.schema.FactType;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for physically implausible values in candidate facts.
 *
 * Rules:
 * - Temperatures: below absolute zero (–273.15°C) are impossible
 * - Pressures: strictly negative values are physically impossible in most vehicle contexts
 * - RPM: negative or astronomically high values (>30,000 RPM) are implausible for passenger vehicles
 * - Percentages: outside [–100%, +200%] are flagged as warnings (some fuel trims can exceed 100%)
 *
 * These checks flag likely extraction errors. They are not substitutes for engineering review.
 * A plausible range does not mean the value is correct — it means it is not obviously impossible.
 *
 * Rejection codes produced:
 * - IMPLAUSIBLE_RANGE — a threshold value is physically impossible or extremely unlikely
 */
public class PlausibilityChecker {

    /**
     * Checks a fact's numeric values for physical plausibility.
     *
     * @param fact The fact to check (only THRESHOLD and EXPECTED_OBSERVATION are checked).
     * @return A CheckResult with PASS, WARN, or REJECT.
     */
    public CheckResult check(CandidateFact fact) {
        if (fact.getFactType() != FactType.THRESHOLD &&
            fact.getFactType() != FactType.EXPECTED_OBSERVATION) {
            return pass(fact, "Plausibility check not applicable to " + fact.getFactType() + " facts.");
        }

        String unit = fact.getUnit();
        if (unit == null) {
            // Field validator will already catch this — just pass here
            return pass(fact, "No unit to check plausibility for.");
        }

        List<String> warnings  = new ArrayList<>();
        List<String> errors    = new ArrayList<>();

        Double lower = fact.getThresholdLower();
        Double upper = fact.getThresholdUpper();

        if ((lower != null && (Double.isNaN(lower) || Double.isInfinite(lower))) ||
            (upper != null && (Double.isNaN(upper) || Double.isInfinite(upper)))) {
            errors.add("Threshold values cannot be NaN or Infinity.");
        }

        // ── Temperature checks ─────────────────────────────────────────────────
        if (isTempUnit(unit)) {
            double minTemp = unit.contains("K") ? 0.0 : -273.15;
            if (lower != null && lower < minTemp) {
                errors.add("Lower threshold " + lower + " " + unit + " is below absolute zero.");
            }
            if (upper != null && upper < minTemp) {
                errors.add("Upper threshold " + upper + " " + unit + " is below absolute zero.");
            }
            // Automotive temperatures: warn if >1000°C (melting point context)
            if (upper != null && toC(upper, unit) > 1000) {
                warnings.add("Upper threshold " + upper + " " + unit +
                             " exceeds 1000°C — unusual for passenger vehicle diagnostics.");
            }
        }

        // ── Pressure checks ────────────────────────────────────────────────────
        if (isPressureUnit(unit)) {
            if (isAbsolutePressureUnit(unit)) {
                if (lower != null && lower < 0) {
                    errors.add("Pressure threshold " + lower + " " + unit +
                               " is negative — physically implausible for absolute pressure.");
                }
                if (upper != null && upper < 0) {
                    errors.add("Pressure threshold " + upper + " " + unit +
                               " is negative — physically implausible for absolute pressure.");
                }
            } else {
                // Gauge pressure can be negative (vacuum) up to approx -1 bar / -14.7 psi
                double minGauge = unit.contains("psi") ? -15.0 : -1.1; // Add some margin
                if (lower != null && lower < minGauge) {
                    errors.add("Gauge pressure threshold " + lower + " " + unit +
                               " implies an impossible physical vacuum.");
                }
            }
        }

        // ── RPM checks ─────────────────────────────────────────────────────────
        if (isRpmUnit(unit)) {
            if (lower != null && lower < 0) {
                errors.add("RPM threshold " + lower + " " + unit + " is negative — implausible.");
            }
            if (upper != null && upper > 30000) {
                warnings.add("RPM threshold " + upper + " " + unit +
                             " exceeds 30,000 RPM — unlikely for passenger vehicle.");
            }
        }

        // ── Percentage checks ──────────────────────────────────────────────────
        if ("%".equals(unit)) {
            if ((lower != null && lower < -100) || (upper != null && upper > 200)) {
                warnings.add("Percentage value outside [–100%, +200%] range. Verify if intentional.");
            }
        }

        // ── Decide ─────────────────────────────────────────────────────────────
        if (!errors.isEmpty()) {
            return CheckResult.builder()
                .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
                .decision(CheckDecision.REJECT)
                .rejectionCodes(List.of("IMPLAUSIBLE_RANGE"))
                .humanExplanation("Plausibility check failed: " + String.join("; ", errors))
                .build();
        }
        if (!warnings.isEmpty()) {
            return CheckResult.builder()
                .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
                .decision(CheckDecision.WARN)
                .rejectionCodes(List.of("IMPLAUSIBLE_RANGE_WARNING"))
                .humanExplanation("Plausibility warning: " + String.join("; ", warnings))
                .build();
        }

        return pass(fact, "Values are within plausible physical ranges for unit '" + unit + "'.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static boolean isTempUnit(String u) {
        return u.equals("°C") || u.equals("C") || u.equals("°F") || u.equals("F") || u.equals("K");
    }

    private static boolean isPressureUnit(String u) {
        String lu = u.toLowerCase();
        return lu.contains("bar") || lu.contains("pa") || lu.contains("psi");
    }

    private static boolean isAbsolutePressureUnit(String u) {
        String lu = u.toLowerCase();
        return lu.contains("bara") || lu.contains("psia") || lu.contains("abs");
    }

    private static boolean isRpmUnit(String u) {
        return u.equals("RPM") || u.equals("rpm");
    }

    private static double toC(double value, String unit) {
        if (unit.equals("°F") || unit.equals("F")) return (value - 32) * 5.0 / 9.0;
        if (unit.equals("K")) return value - 273.15;
        return value;
    }

    private static CheckResult pass(CandidateFact fact, String msg) {
        return CheckResult.builder()
            .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
            .decision(CheckDecision.PASS)
            .humanExplanation(msg)
            .build();
    }
}
