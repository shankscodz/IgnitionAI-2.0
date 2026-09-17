package com.ignitionai.factextraction.schema;

import java.util.List;
import java.util.Objects;

/**
 * Output contract returned by the fact extraction pipeline for one passage.
 *
 * A single passage may yield zero or more candidate facts.
 * Extraction failures are recorded explicitly with reasons, not silently swallowed.
 */
public final class ExtractionResult {

    public enum Outcome {
        /** One or more candidate facts were extracted from the passage. */
        FACTS_EXTRACTED,
        /** No facts could be extracted (passage had no structured knowledge of extractable types). */
        NO_FACTS_FOUND,
        /** Extraction failed due to a processing error. */
        EXTRACTION_FAILED,
        /** Passage was too short or content-free to extract meaningful facts. */
        INSUFFICIENT_CONTENT
    }

    private final Outcome outcome;

    /** The passageId that was processed. */
    private final String passageId;

    /** The sourceId of the document containing the passage. */
    private final String sourceId;

    /**
     * All candidate facts extracted from this passage.
     * Multiple facts may reference the same passageId.
     * Competing facts from different sources are preserved, not merged.
     */
    private final List<CandidateFact> facts;

    /** Human-readable explanation of the outcome (especially for failures). */
    private final String explanation;

    /** Machine-readable failure code (null for successful extractions). */
    private final String failureCode;

    private ExtractionResult(Builder builder) {
        this.outcome     = Objects.requireNonNull(builder.outcome, "outcome is required");
        this.passageId   = Objects.requireNonNull(builder.passageId, "passageId is required");
        this.sourceId    = Objects.requireNonNull(builder.sourceId, "sourceId is required");
        this.facts       = builder.facts != null ? List.copyOf(builder.facts) : List.of();
        this.explanation = Objects.requireNonNull(builder.explanation, "explanation is required");
        this.failureCode = builder.failureCode;
    }

    public Outcome getOutcome()            { return outcome; }
    public String getPassageId()           { return passageId; }
    public String getSourceId()            { return sourceId; }
    public List<CandidateFact> getFacts()  { return facts; }
    public String getExplanation()         { return explanation; }
    public String getFailureCode()         { return failureCode; }
    public int getFactCount()              { return facts.size(); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Outcome outcome;
        private String passageId;
        private String sourceId;
        private List<CandidateFact> facts;
        private String explanation;
        private String failureCode;

        public Builder outcome(Outcome v)              { this.outcome = v; return this; }
        public Builder passageId(String v)             { this.passageId = v; return this; }
        public Builder sourceId(String v)              { this.sourceId = v; return this; }
        public Builder facts(List<CandidateFact> v)    { this.facts = v; return this; }
        public Builder explanation(String v)           { this.explanation = v; return this; }
        public Builder failureCode(String v)           { this.failureCode = v; return this; }

        public ExtractionResult build() { return new ExtractionResult(this); }
    }

    @Override
    public String toString() {
        return "ExtractionResult{outcome=" + outcome + ", passageId='" + passageId +
               "', factsCount=" + facts.size() + "}";
    }
}
