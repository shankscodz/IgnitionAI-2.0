package com.ignitionai.consistencychecks.service;

import com.ignitionai.consistencychecks.checks.*;
import com.ignitionai.consistencychecks.schema.*;
import com.ignitionai.factextraction.schema.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates all consistency checks on candidate facts.
 *
 * Pipeline per fact:
 * 1. FieldValidator       — required fields, units, numeric types
 * 2. ReferenceValidator   — sourceId existence, passageId format, applicability wildcards
 * 3. PlausibilityChecker  — physical plausibility of numeric values
 * 4. ThresholdConflictDetector — conflicts with other checked threshold facts
 *
 * Status transitions applied:
 * CANDIDATE → CHECKED    (all checks PASS or WARN only)
 * CANDIDATE → REJECTED   (any REJECT)
 * CANDIDATE → QUARANTINED (any QUARANTINE from conflict detector)
 *
 * IMPORTANT: CHECKED status means "passed automated checks."
 * It does NOT mean the fact is ready for publication.
 * Human review (REVIEWED status) is a separate explicit step.
 *
 * Quarantined and rejected facts are preserved with their check results.
 * They are never silently discarded.
 */
public class ConsistencyCheckService {

    private final FieldValidator fieldValidator;
    private final ReferenceValidator referenceValidator;
    private final PlausibilityChecker plausibilityChecker;
    private final ThresholdConflictDetector conflictDetector;

    /** Store of all check results, keyed by factId. */
    private final Map<String, CheckResult> checkResults = new LinkedHashMap<>();

    /** Clean store: facts that passed all checks (CHECKED status). */
    private final Map<String, CandidateFact> checkedFacts = new LinkedHashMap<>();

    /** Rejected store: facts with REJECT decision — structural/reference/plausibility failures. */
    private final Map<String, CandidateFact> rejectedFacts = new LinkedHashMap<>();

    /** Quarantine store: facts with conflicts — never averaged, held for explicit resolution. */
    private final Map<String, CandidateFact> quarantinedFacts = new LinkedHashMap<>();

    public ConsistencyCheckService() {
        this.fieldValidator       = new FieldValidator();
        this.referenceValidator   = new ReferenceValidator();
        this.plausibilityChecker  = new PlausibilityChecker();
        this.conflictDetector     = new ThresholdConflictDetector();
    }

    /**
     * Runs all consistency checks on a single candidate fact.
     *
     * @param fact           The candidate fact to check.
     * @param knownSourceIds Set of sourceIds known to exist in technical-sources store.
     * @return The combined CheckResult for this fact.
     */
    public CheckResult check(CandidateFact fact, Set<String> knownSourceIds) {
        Objects.requireNonNull(fact, "fact is required");

        // ── Step 1: Field validation ──────────────────────────────────────────
        CheckResult fieldResult = fieldValidator.validate(fact);
        if (fieldResult.isRejected()) {
            return finalise(fact, fieldResult, FactStatus.REJECTED);
        }

        // ── Step 2: Reference validation ─────────────────────────────────────
        CheckResult refResult = referenceValidator.validate(fact, knownSourceIds);
        if (refResult.isRejected()) {
            return finalise(fact, refResult, FactStatus.REJECTED);
        }

        // ── Step 3: Plausibility checks ───────────────────────────────────────
        CheckResult plausResult = plausibilityChecker.check(fact);
        if (plausResult.isRejected()) {
            return finalise(fact, plausResult, FactStatus.REJECTED);
        }

        // ── Step 4: Threshold conflict detection ──────────────────────────────
        List<CandidateFact> existingThresholdFacts = List.copyOf(checkedFacts.values());
        CheckResult conflictResult = conflictDetector.check(fact, existingThresholdFacts);
        if (conflictResult.isQuarantined()) {
            return finalise(fact, conflictResult, FactStatus.QUARANTINED);
        }

        // ── All checks passed (possibly with warnings) ────────────────────────
        CheckResult combinedResult = combinePassed(fact, fieldResult, refResult, plausResult, conflictResult);
        return finalise(fact, combinedResult, FactStatus.CHECKED);
    }

    /**
     * Runs consistency checks on a list of candidate facts in sequence.
     * Later facts are checked against earlier checked facts for conflict detection.
     *
     * @param facts          The list of candidate facts to check (order matters for conflict detection).
     * @param knownSourceIds Known sourceIds from technical-sources store.
     * @return Map of factId → CheckResult.
     */
    public Map<String, CheckResult> checkAll(List<CandidateFact> facts, Set<String> knownSourceIds) {
        Map<String, CheckResult> results = new LinkedHashMap<>();
        for (CandidateFact fact : facts) {
            CheckResult result = check(fact, knownSourceIds);
            results.put(fact.getFactId(), result);
        }
        System.out.printf("[ConsistencyCheckService] Checked %d facts: %d checked, %d rejected, %d quarantined.%n",
            facts.size(), checkedFacts.size(), rejectedFacts.size(), quarantinedFacts.size());
        return results;
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** All facts that passed automated checks. Status: CHECKED. Not yet published. */
    public List<CandidateFact> getCheckedFacts()      { return List.copyOf(checkedFacts.values()); }

    /** All facts that failed automated checks. Status: REJECTED. Preserved with reasons. */
    public List<CandidateFact> getRejectedFacts()     { return List.copyOf(rejectedFacts.values()); }

    /** All facts quarantined due to conflicts. Never averaged. Status: QUARANTINED. */
    public List<CandidateFact> getQuarantinedFacts()  { return List.copyOf(quarantinedFacts.values()); }

    /** Retrieves the check result for a specific fact. */
    public Optional<CheckResult> getCheckResult(String factId) {
        return Optional.ofNullable(checkResults.get(factId));
    }

    /** Retrieves facts in the conflict/review queue (quarantined + rejected). */
    public List<CandidateFact> getConflictReviewQueue() {
        List<CandidateFact> queue = new ArrayList<>();
        queue.addAll(quarantinedFacts.values());
        queue.addAll(rejectedFacts.values());
        return Collections.unmodifiableList(queue);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private CheckResult finalise(CandidateFact fact, CheckResult result, FactStatus newStatus) {
        checkResults.put(fact.getFactId(), result);
        CandidateFact updatedFact = fact.withStatus(newStatus);
        switch (newStatus) {
            case CHECKED     -> checkedFacts.put(fact.getFactId(), updatedFact);
            case REJECTED    -> rejectedFacts.put(fact.getFactId(), updatedFact);
            case QUARANTINED -> quarantinedFacts.put(fact.getFactId(), updatedFact);
            default          -> throw new IllegalStateException("Unexpected status: " + newStatus);
        }
        return result;
    }

    private CheckResult combinePassed(CandidateFact fact, CheckResult... results) {
        List<String> codes = Arrays.stream(results)
            .flatMap(r -> r.getRejectionCodes().stream()).collect(Collectors.toList());
        boolean hasWarnings = Arrays.stream(results)
            .anyMatch(r -> r.getDecision() == CheckDecision.WARN);
        String explanation = "All checks passed" + (hasWarnings ? " with warnings" : "") + ". " +
            "Fact is CHECKED — human review required before publication.";
        return CheckResult.builder()
            .factId(fact.getFactId()).sourceId(fact.getSourceId()).passageId(fact.getPassageId())
            .decision(hasWarnings ? CheckDecision.WARN : CheckDecision.PASS)
            .rejectionCodes(codes)
            .humanExplanation(explanation)
            .build();
    }
}
