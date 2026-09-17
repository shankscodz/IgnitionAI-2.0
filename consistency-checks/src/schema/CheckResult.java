package com.ignitionai.consistencychecks.schema;

import java.util.List;
import java.util.Objects;

/**
 * Result of running all consistency checks on a single {@link CandidateFact}.
 *
 * Carries the check decision, machine-readable rejection/warning codes,
 * a human-readable explanation, and the IDs of conflicting facts (for quarantine cases).
 *
 * Provenance is preserved through this result: the original factId, sourceId and passageId
 * are never modified by the check process.
 *
 * IMPORTANT: A PASS decision here means "passed automated checks."
 * It does NOT mean the fact is technically reviewed or approved for publication.
 * Human review (REVIEWED status) is a separate explicit step.
 */
public final class CheckResult {

    /** The factId of the fact that was checked. */
    private final String factId;

    /** The sourceId of the checked fact (for traceability). */
    private final String sourceId;

    /** The passageId of the checked fact (for traceability). */
    private final String passageId;

    /** The check decision outcome. */
    private final CheckDecision decision;

    /**
     * Machine-readable rejection/warning codes.
     * Examples: "MISSING_UNIT", "UNKNOWN_VEHICLE_REFERENCE", "THRESHOLD_CONFLICT", "IMPLAUSIBLE_RANGE"
     * Empty for a clean PASS.
     */
    private final List<String> rejectionCodes;

    /**
     * Human-readable explanation of the check outcome.
     * For REJECT and QUARANTINE: explains what was wrong and what the checker expected.
     * For PASS/WARN: describes what was checked and any warnings.
     */
    private final String humanExplanation;

    /**
     * For QUARANTINE decisions: the factIds of conflicting facts.
     * These are the other facts this fact conflicts with, preserved for resolution.
     */
    private final List<String> conflictingFactIds;

    private CheckResult(Builder builder) {
        this.factId            = Objects.requireNonNull(builder.factId, "factId is required");
        this.sourceId          = Objects.requireNonNull(builder.sourceId, "sourceId is required");
        this.passageId         = Objects.requireNonNull(builder.passageId, "passageId is required");
        this.decision          = Objects.requireNonNull(builder.decision, "decision is required");
        this.humanExplanation  = Objects.requireNonNull(builder.humanExplanation, "humanExplanation is required");
        this.rejectionCodes    = builder.rejectionCodes != null ? List.copyOf(builder.rejectionCodes) : List.of();
        this.conflictingFactIds = builder.conflictingFactIds != null ? List.copyOf(builder.conflictingFactIds) : List.of();
    }

    public String getFactId()                   { return factId; }
    public String getSourceId()                 { return sourceId; }
    public String getPassageId()                { return passageId; }
    public CheckDecision getDecision()          { return decision; }
    public List<String> getRejectionCodes()     { return rejectionCodes; }
    public String getHumanExplanation()         { return humanExplanation; }
    public List<String> getConflictingFactIds() { return conflictingFactIds; }

    public boolean isPassed()      { return decision == CheckDecision.PASS; }
    public boolean isRejected()    { return decision == CheckDecision.REJECT; }
    public boolean isQuarantined() { return decision == CheckDecision.QUARANTINE; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String factId;
        private String sourceId;
        private String passageId;
        private CheckDecision decision;
        private List<String> rejectionCodes;
        private String humanExplanation;
        private List<String> conflictingFactIds;

        public Builder factId(String v)                      { this.factId = v; return this; }
        public Builder sourceId(String v)                    { this.sourceId = v; return this; }
        public Builder passageId(String v)                   { this.passageId = v; return this; }
        public Builder decision(CheckDecision v)             { this.decision = v; return this; }
        public Builder rejectionCodes(List<String> v)        { this.rejectionCodes = v; return this; }
        public Builder humanExplanation(String v)            { this.humanExplanation = v; return this; }
        public Builder conflictingFactIds(List<String> v)    { this.conflictingFactIds = v; return this; }

        public CheckResult build() { return new CheckResult(this); }
    }

    @Override
    public String toString() {
        return "CheckResult{factId='" + factId + "', decision=" + decision +
               ", codes=" + rejectionCodes + "}";
    }
}
