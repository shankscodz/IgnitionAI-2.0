package com.ignitionai.consistencychecks.checks;

import com.ignitionai.consistencychecks.schema.CheckDecision;
import com.ignitionai.consistencychecks.schema.CheckResult;
import com.ignitionai.factextraction.schema.CandidateFact;
import com.ignitionai.factextraction.schema.FactType;

import java.util.*;

/**
 * Detects incompatible threshold values among THRESHOLD facts that overlap in both
 * vehicle applicability and operating conditions.
 *
 * DESIGN RULE: Contradictions are QUARANTINED, never averaged or silently discarded.
 * Different conditions may legitimately produce different thresholds — the conflict
 * detector only flags facts that overlap on BOTH applicability AND conditions.
 *
 * Rejection codes produced:
 * - THRESHOLD_CONFLICT — two or more THRESHOLD facts for the same quantity,
 *   vehicle applicability and operating conditions have incompatible numeric ranges.
 */
public class ThresholdConflictDetector {

    /**
     * Checks a candidate fact against a set of already-checked facts to detect threshold conflicts.
     *
     * @param fact          The THRESHOLD fact to check.
     * @param existingFacts All previously checked THRESHOLD facts (for comparison).
     * @return A CheckResult. PASS if no conflict, QUARANTINE if a conflict is detected.
     */
    public CheckResult check(CandidateFact fact, List<CandidateFact> existingFacts) {
        if (fact.getFactType() != FactType.THRESHOLD) {
            // This check only applies to THRESHOLD facts
            return CheckResult.builder()
                .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
                .decision(CheckDecision.PASS)
                .humanExplanation("Threshold conflict check not applicable to " + fact.getFactType() + " facts.")
                .build();
        }

        List<String> conflictingIds   = new ArrayList<>();
        List<String> explanations     = new ArrayList<>();

        for (CandidateFact existing : existingFacts) {
            if (existing.getFactType() != FactType.THRESHOLD) continue;
            if (existing.getFactId().equals(fact.getFactId())) continue;

            // Only compare facts for the same canonical quantity and role
            if (!Objects.equals(fact.getQuantityId(), existing.getQuantityId())) continue;
            if (!Objects.equals(fact.getThresholdRole(), existing.getThresholdRole())) continue;

            // Only compare facts that overlap in BOTH vehicle applicability AND operating conditions
            if (!applicabilityOverlaps(fact, existing)) continue;
            if (!conditionsOverlap(fact, existing)) continue;

            // Check for numeric incompatibility (handles unit normalization)
            if (rangesAreIncompatible(fact, existing)) {
                conflictingIds.add(existing.getFactId());
                explanations.add(
                    "Fact " + fact.getFactId() + " [" + thresholdSummary(fact) + "] " +
                    "conflicts with fact " + existing.getFactId() + " [" + thresholdSummary(existing) + "] " +
                    "for quantity='" + fact.getQuantityId() + "' role='" + fact.getThresholdRole() + "' " +
                    "applicability='" + fact.getVehicleApplicability() + "' " +
                    "conditions='" + fact.getOperatingConditions() + "'. " +
                    "Quarantined — do not average.");
            }
        }

        if (!conflictingIds.isEmpty()) {
            return CheckResult.builder()
                .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
                .decision(CheckDecision.QUARANTINE)
                .rejectionCodes(List.of("THRESHOLD_CONFLICT"))
                .conflictingFactIds(conflictingIds)
                .humanExplanation("THRESHOLD_CONFLICT: " + String.join("; ", explanations))
                .build();
        }

        return CheckResult.builder()
            .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
            .decision(CheckDecision.PASS)
            .humanExplanation("No threshold conflicts detected.")
            .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Two facts' vehicle applicability overlaps when both are null (undetermined),
     * or both are non-null and share a common substring (simple MVP heuristic).
     * Production implementation would use the VehicleApplicability registry for proper matching.
     */
    private boolean applicabilityOverlaps(CandidateFact a, CandidateFact b) {
        String va = a.getVehicleApplicability();
        String vb = b.getVehicleApplicability();
        if (va == null && vb == null) return true; // both undetermined — potentially overlapping
        if (va == null || vb == null) return false; // one is specific, one is undetermined — no forced overlap
        // Simple overlap: if either is a substring of the other
        return va.toLowerCase(Locale.ROOT).contains(vb.toLowerCase(Locale.ROOT)) ||
               vb.toLowerCase(Locale.ROOT).contains(va.toLowerCase(Locale.ROOT));
    }

    /**
     * Two facts' operating conditions overlap when both are null,
     * or both are non-null and share a common term.
     */
    private boolean conditionsOverlap(CandidateFact a, CandidateFact b) {
        String ca = a.getOperatingConditions();
        String cb = b.getOperatingConditions();
        if (ca == null && cb == null) return true;
        if (ca == null || cb == null) return false;
        return ca.toLowerCase(Locale.ROOT).contains(cb.toLowerCase(Locale.ROOT)) ||
               cb.toLowerCase(Locale.ROOT).contains(ca.toLowerCase(Locale.ROOT));
    }

    private boolean rangesAreIncompatible(CandidateFact a, CandidateFact b) {
        String unitA = a.getUnit() != null ? a.getUnit().toLowerCase(Locale.ROOT) : "";
        String unitB = b.getUnit() != null ? b.getUnit().toLowerCase(Locale.ROOT) : "";

        boolean isTempA = unitA.contains("c") || unitA.contains("f") || unitA.contains("k");
        boolean isTempB = unitB.contains("c") || unitB.contains("f") || unitB.contains("k");

        // If units are fundamentally different and not temperatures, we don't compare them here.
        if (!unitA.equals(unitB) && !(isTempA && isTempB)) {
            return false;
        }

        double aLow  = normalizeTemp(a.getThresholdLower(), unitA);
        double aHigh = normalizeTemp(a.getThresholdUpper(), unitA);
        double bLow  = normalizeTemp(b.getThresholdLower(), unitB);
        double bHigh = normalizeTemp(b.getThresholdUpper(), unitB);

        aLow  = Double.isNaN(aLow)  ? Double.NEGATIVE_INFINITY : aLow;
        aHigh = Double.isNaN(aHigh) ? Double.POSITIVE_INFINITY : aHigh;
        bLow  = Double.isNaN(bLow)  ? Double.NEGATIVE_INFINITY : bLow;
        bHigh = Double.isNaN(bHigh) ? Double.POSITIVE_INFINITY : bHigh;

        // Incompatible: ranges do not overlap at all
        return aHigh < bLow || bHigh < aLow;
    }

    private double normalizeTemp(Double value, String unit) {
        if (value == null) return Double.NaN;
        if (unit == null) return value;
        String u = unit.toLowerCase(Locale.ROOT);
        if (u.contains("f") && !u.contains("c")) {
            // Fahrenheit to Celsius
            return (value - 32) * 5.0 / 9.0;
        }
        if (u.contains("k")) {
            // Kelvin to Celsius
            return value - 273.15;
        }
        return value; // Assume Celsius or non-temp
    }

    private String thresholdSummary(CandidateFact f) {
        String low  = f.getThresholdLower() != null ? String.valueOf(f.getThresholdLower()) : "–∞";
        String high = f.getThresholdUpper() != null ? String.valueOf(f.getThresholdUpper()) : "+∞";
        return "[" + low + ", " + high + "] " + f.getUnit();
    }
}
