package com.ignitionai.consistencychecks.schema;

/**
 * Decision outcome for a consistency check run on a {@link CandidateFact}.
 */
public enum CheckDecision {
    /** Fact passed all automated checks. Ready for human review (CHECKED status). */
    PASS,
    /** Fact has a non-blocking issue that should be noted but does not block CHECKED status. */
    WARN,
    /** Fact has a structural, reference, or plausibility error. Must not proceed to CHECKED. */
    REJECT,
    /**
     * Fact conflicts with another fact for the same vehicle applicability and operating conditions.
     * Quarantined — not averaged, not discarded, held for explicit resolution.
     */
    QUARANTINE
}
