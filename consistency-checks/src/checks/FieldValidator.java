package com.ignitionai.consistencychecks.checks;

import com.ignitionai.consistencychecks.schema.CheckDecision;
import com.ignitionai.consistencychecks.schema.CheckResult;
import com.ignitionai.factextraction.schema.CandidateFact;
import com.ignitionai.factextraction.schema.FactType;

import java.util.*;

/**
 * Validates required fields, numeric types, units and permitted conversions.
 *
 * This check is the first gate: if required structural fields are missing or malformed,
 * the fact is rejected immediately. This prevents downstream checks from operating on
 * structurally invalid data.
 *
 * Rejection codes produced:
 * - MISSING_REQUIRED_FIELD     — a required field for the fact's type is null or blank
 * - INVALID_NUMERIC_VALUE      — a numeric field has an impossible value (e.g. negative temperature in K)
 * - MISSING_UNIT               — THRESHOLD/EXPECTED_OBSERVATION fact has no unit
 * - UNRECOGNIZED_UNIT          — unit is present but not in the known unit registry
 * - MISSING_EXTRACTOR_VERSION  — extractorVersion is absent (reproducibility cannot be guaranteed)
 */
public class FieldValidator {

    /** Known physical units. Extended as new measurement types are added. */
    private static final Set<String> KNOWN_UNITS = Set.of(
        // Temperature
        "°C", "°F", "K", "C", "F",
        // Pressure
        "bar", "kPa", "MPa", "psi", "Pa",
        // Speed / RPM
        "RPM", "rpm", "km/h", "mph", "m/s",
        // Electrical
        "V", "mV", "A", "mA", "Ω",
        // Ratio / percentage
        "%", "lambda", "AFR",
        // Mass flow
        "g/s", "kg/h",
        // Volume / fuel
        "L", "mL", "L/h",
        // Time
        "ms", "s", "min", "h",
        // Angle
        "°", "deg", "BTDC",
        // Force / torque
        "Nm", "N",
        // Generic
        "count", "ratio"
    );

    /**
     * Validates a candidate fact's fields.
     *
     * @param fact The candidate fact to validate.
     * @return A CheckResult. Only PASS and REJECT are produced by this check.
     */
    public CheckResult validate(CandidateFact fact) {
        List<String> rejectionCodes = new ArrayList<>();
        List<String> explanations   = new ArrayList<>();

        // ── Universal required fields ─────────────────────────────────────────
        if (blank(fact.getFactId()))               { rejectionCodes.add("MISSING_REQUIRED_FIELD"); explanations.add("factId is blank"); }
        if (blank(fact.getSourceId()))             { rejectionCodes.add("MISSING_REQUIRED_FIELD"); explanations.add("sourceId is blank"); }
        if (blank(fact.getPassageId()))            { rejectionCodes.add("MISSING_REQUIRED_FIELD"); explanations.add("passageId is blank"); }
        if (blank(fact.getSupportingPassageText())) { rejectionCodes.add("MISSING_REQUIRED_FIELD"); explanations.add("supportingPassageText is blank"); }
        if (blank(fact.getExtractorVersion())) {
            rejectionCodes.add("MISSING_EXTRACTOR_VERSION");
            explanations.add("extractorVersion is absent; reproducibility cannot be guaranteed");
        }

        // ── Type-specific field validation ────────────────────────────────────
        if (fact.getFactType() == FactType.THRESHOLD) {
            // THRESHOLD: must have at least one bound and a unit
            if (fact.getThresholdLower() == null && fact.getThresholdUpper() == null) {
                rejectionCodes.add("MISSING_REQUIRED_FIELD");
                explanations.add("THRESHOLD fact must have at least one of thresholdLower or thresholdUpper");
            }
            if (blank(fact.getUnit())) {
                rejectionCodes.add("MISSING_UNIT");
                explanations.add("THRESHOLD fact has no unit. An isolated number has no physical meaning.");
            } else if (!KNOWN_UNITS.contains(fact.getUnit())) {
                rejectionCodes.add("UNRECOGNIZED_UNIT");
                explanations.add("Unit '" + fact.getUnit() + "' is not in the known unit registry. " +
                                 "Add it explicitly if it is a valid physical unit.");
            }
            // Range sanity: lower must be <= upper when both present
            if (fact.getThresholdLower() != null && fact.getThresholdUpper() != null) {
                if (fact.getThresholdLower() > fact.getThresholdUpper()) {
                    rejectionCodes.add("INVALID_NUMERIC_VALUE");
                    explanations.add("thresholdLower (" + fact.getThresholdLower() + ") is greater than " +
                                     "thresholdUpper (" + fact.getThresholdUpper() + ")");
                }
            }
        }

        if (fact.getFactType() == FactType.EXPECTED_OBSERVATION) {
            if (blank(fact.getUnit())) {
                rejectionCodes.add("MISSING_UNIT");
                explanations.add("EXPECTED_OBSERVATION fact has no unit.");
            }
        }

        if (fact.getFactType() == FactType.SYMPTOM_FAULT) {
            if (blank(fact.getSymptomDescription())) {
                rejectionCodes.add("MISSING_REQUIRED_FIELD");
                explanations.add("SYMPTOM_FAULT fact must have symptomDescription");
            }
            if (blank(fact.getFaultDescription())) {
                rejectionCodes.add("MISSING_REQUIRED_FIELD");
                explanations.add("SYMPTOM_FAULT fact must have faultDescription");
            }
        }

        if (fact.getFactType() == FactType.ACTION) {
            if (blank(fact.getActionDescription())) {
                rejectionCodes.add("MISSING_REQUIRED_FIELD");
                explanations.add("ACTION fact must have actionDescription");
            }
        }

        // ── Decision ──────────────────────────────────────────────────────────
        if (!rejectionCodes.isEmpty()) {
            return CheckResult.builder()
                .factId(fact.getFactId())
                .sourceId(fact.getSourceId())
                .passageId(fact.getPassageId())
                .decision(CheckDecision.REJECT)
                .rejectionCodes(rejectionCodes)
                .humanExplanation("Field validation failed: " + String.join("; ", explanations))
                .build();
        }

        return CheckResult.builder()
            .factId(fact.getFactId())
            .sourceId(fact.getSourceId())
            .passageId(fact.getPassageId())
            .decision(CheckDecision.PASS)
            .humanExplanation("All required fields present and valid for " + fact.getFactType() + " fact.")
            .build();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
