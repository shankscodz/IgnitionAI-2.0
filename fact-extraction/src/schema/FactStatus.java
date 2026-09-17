package com.ignitionai.factextraction.schema;

/**
 * Lifecycle status of a {@link CandidateFact}.
 *
 * Facts move through these states as they progress through the knowledge pipeline.
 * Passing automated consistency checks does NOT automatically promote a fact to REVIEWED
 * or PUBLISHED — those require explicit human/technician action.
 *
 * Transitions:
 *   CANDIDATE → CHECKED   (passed automated consistency checks)
 *   CANDIDATE → REJECTED  (failed consistency checks)
 *   CHECKED   → REVIEWED  (human/technician review completed)
 *   REVIEWED  → PUBLISHED (promoted to live registry)
 *   CHECKED   → REJECTED  (failed human review)
 *   any       → QUARANTINED (contradicts another fact; held pending resolution)
 */
public enum FactStatus {

    /** Newly extracted; awaiting consistency checks. */
    CANDIDATE,

    /** Passed automated consistency checks. Awaiting human review. */
    CHECKED,

    /** Reviewed and approved by a human/technician. Eligible for publication. */
    REVIEWED,

    /** Published to the linked registries. Immutable from this point. */
    PUBLISHED,

    /** Failed consistency checks or human review. Not eligible for publication. */
    REJECTED,

    /**
     * Contradicts another fact for the same vehicle applicability and conditions.
     * Held in the conflict queue pending explicit resolution; not averaged or silently discarded.
     */
    QUARANTINED
}
