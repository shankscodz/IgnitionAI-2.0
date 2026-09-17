package com.ignitionai.consistencychecks.checks;

import com.ignitionai.consistencychecks.schema.CheckDecision;
import com.ignitionai.consistencychecks.schema.CheckResult;
import com.ignitionai.factextraction.schema.CandidateFact;

import java.util.*;

/**
 * Validates that references within a fact (sourceIds, vehicleApplicability patterns,
 * known observation signal IDs) resolve to known entities.
 *
 * For the Phase 1 MVP, this validator checks:
 * 1. sourceId format is valid and non-blank (cross-module reference to technical-sources)
 * 2. passageId format is consistent with its sourceId (format: "{sourceId}#P{seq}")
 * 3. vehicleApplicability, if present, is not a wildcard or obviously invalid value
 *
 * Rejection codes produced:
 * - INVALID_SOURCE_REFERENCE    — sourceId or passageId format is malformed
 * - INCONSISTENT_PASSAGE_REF    — passageId does not reference the stated sourceId
 * - INVALID_VEHICLE_APPLICABILITY — vehicleApplicability is obviously malformed
 */
public class ReferenceValidator {

    // Minimum prefix expected on source IDs
    private static final String SOURCE_ID_PREFIX = "SRC-";
    private static final String FACT_ID_PREFIX   = "FACT-";

    /**
     * Validates cross-references within a candidate fact.
     *
     * @param fact The candidate fact to validate.
     * @param knownSourceIds Set of sourceIds known to exist in the technical-sources store.
     * @return A CheckResult. Only PASS, WARN, and REJECT are produced by this check.
     */
    public CheckResult validate(CandidateFact fact, Set<String> knownSourceIds) {
        List<String> rejectionCodes = new ArrayList<>();
        List<String> explanations   = new ArrayList<>();

        // 1. sourceId format check
        if (!fact.getSourceId().startsWith(SOURCE_ID_PREFIX)) {
            rejectionCodes.add("INVALID_SOURCE_REFERENCE");
            explanations.add("sourceId '" + fact.getSourceId() + "' does not match expected format 'SRC-NNNNN'");
        }

        // 2. sourceId existence check (against known sources)
        if (knownSourceIds != null && !knownSourceIds.isEmpty() &&
                !knownSourceIds.contains(fact.getSourceId())) {
            rejectionCodes.add("INVALID_SOURCE_REFERENCE");
            explanations.add("sourceId '" + fact.getSourceId() + "' does not exist in the technical-sources store");
        }

        // 3. passageId consistency: must start with the sourceId
        if (fact.getPassageId() != null && !fact.getPassageId().isBlank()) {
            if (!fact.getPassageId().startsWith(fact.getSourceId() + "#P")) {
                rejectionCodes.add("INCONSISTENT_PASSAGE_REF");
                explanations.add("passageId '" + fact.getPassageId() + "' does not reference sourceId '" +
                                 fact.getSourceId() + "'. Expected format: '" + fact.getSourceId() + "#P{seq}'");
            }
        }

        // 4. vehicleApplicability: warn on wildcards like "*" or "all" or "any"
        String va = fact.getVehicleApplicability();
        if (va != null) {
            String lower = va.toLowerCase(Locale.ROOT);
            if (lower.equals("*") || lower.equals("all") || lower.equals("any") ||
                    lower.equals("all vehicles") || lower.equals("universal")) {
                rejectionCodes.add("INVALID_VEHICLE_APPLICABILITY");
                explanations.add("vehicleApplicability '" + va + "' is a forbidden wildcard. " +
                                 "Use a specific vehicle scope or leave null if undetermined.");
            }
        }

        if (!rejectionCodes.isEmpty()) {
            return CheckResult.builder()
                .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
                .decision(CheckDecision.REJECT)
                .rejectionCodes(rejectionCodes)
                .humanExplanation("Reference validation failed: " + String.join("; ", explanations))
                .build();
        }

        return CheckResult.builder()
            .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
            .decision(CheckDecision.PASS)
            .humanExplanation("All cross-references are valid.")
            .build();
    }
}
