package com.ignitionai.technicalsources.schema;

import java.util.Objects;

/**
 * Output contract returned by the ingestion pipeline for each document processed.
 *
 * Carries the outcome decision plus the stored {@link TechnicalSource} record (if ingested)
 * and a human-readable reason for the outcome.
 */
public final class IngestionResult {

    public enum Outcome {
        /** Document successfully ingested; source record and passages are available. */
        INGESTED,
        /** Document already exists (same hash). Existing source record is referenced. */
        DUPLICATE,
        /** Document could not be read as text. */
        UNREADABLE,
        /** Required metadata was missing and could not be determined. */
        MISSING_METADATA,
        /** File path did not exist or was inaccessible. */
        NOT_FOUND
    }

    private final Outcome outcome;

    /**
     * The stored source record.
     * Populated for INGESTED and DUPLICATE outcomes.
     * For DUPLICATE, this is the existing matching record, not a new one.
     */
    private final TechnicalSource sourceRecord;

    /** Machine-readable rejection code (null for INGESTED). */
    private final String rejectionCode;

    /** Human-readable explanation of the outcome. */
    private final String explanation;

    /** The file path that was attempted (for error reporting). */
    private final String attemptedPath;

    private IngestionResult(Builder builder) {
        this.outcome        = Objects.requireNonNull(builder.outcome, "outcome is required");
        this.sourceRecord   = builder.sourceRecord;
        this.rejectionCode  = builder.rejectionCode;
        this.explanation    = Objects.requireNonNull(builder.explanation, "explanation is required");
        this.attemptedPath  = builder.attemptedPath;
    }

    public Outcome getOutcome()              { return outcome; }
    public TechnicalSource getSourceRecord() { return sourceRecord; }
    public String getRejectionCode()         { return rejectionCode; }
    public String getExplanation()           { return explanation; }
    public String getAttemptedPath()         { return attemptedPath; }
    public boolean isSuccessful()            { return outcome == Outcome.INGESTED || outcome == Outcome.DUPLICATE; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Outcome outcome;
        private TechnicalSource sourceRecord;
        private String rejectionCode;
        private String explanation;
        private String attemptedPath;

        public Builder outcome(Outcome outcome)                  { this.outcome = outcome; return this; }
        public Builder sourceRecord(TechnicalSource sourceRecord) { this.sourceRecord = sourceRecord; return this; }
        public Builder rejectionCode(String rejectionCode)       { this.rejectionCode = rejectionCode; return this; }
        public Builder explanation(String explanation)           { this.explanation = explanation; return this; }
        public Builder attemptedPath(String attemptedPath)       { this.attemptedPath = attemptedPath; return this; }

        public IngestionResult build() { return new IngestionResult(this); }
    }

    @Override
    public String toString() {
        return "IngestionResult{outcome=" + outcome + ", source=" +
               (sourceRecord != null ? sourceRecord.getSourceId() : "null") +
               ", explanation='" + explanation + "'}";
    }
}
