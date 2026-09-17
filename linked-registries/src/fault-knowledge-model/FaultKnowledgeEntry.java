package com.ignitionai.linkedregistries.schema;

import java.util.List;
import java.util.Objects;

/**
 * A published entry in the Fault Knowledge Model sub-registry.
 *
 * Records a supported fault/symptom/observation relationship and the
 * conditions under which it holds. Each entry carries evidence links
 * back to source passages and facts.
 */
public final class FaultKnowledgeEntry {

    /** Stable unique ID for this fault knowledge entry. */
    private final String entryId;

    /** The fault being described (e.g. "Coolant System — Thermostat Failure"). */
    private final String faultName;

    /** Fault code if applicable (e.g. DTC "P0128"). Null if no code is associated. */
    private final String faultCode;

    /** Vehicle applicability entry ID this fault knowledge applies to. Cross-registry reference. */
    private final String vehicleApplicabilityEntryId;

    /** Observable symptoms that may indicate this fault. */
    private final List<String> symptoms;

    /**
     * Observation IDs from the Observation Registry that are relevant to this fault.
     * Cross-registry reference — these IDs must exist in the Observation Registry.
     */
    private final List<String> relevantObservationIds;

    /** Conditions under which this fault/symptom relationship holds. */
    private final String conditions;

    /** Human-readable description of the fault and its diagnostic significance. */
    private final String description;

    /** The factId(s) of the REVIEWED facts that produced this entry. */
    private final List<String> sourceFactIds;

    /** The passageId(s) that supported the facts. */
    private final List<String> sourcePassageIds;

    private FaultKnowledgeEntry(Builder builder) {
        this.entryId                    = Objects.requireNonNull(builder.entryId, "entryId is required");
        this.faultName                  = Objects.requireNonNull(builder.faultName, "faultName is required");
        this.vehicleApplicabilityEntryId = builder.vehicleApplicabilityEntryId;
        this.faultCode                  = builder.faultCode;
        this.symptoms                   = builder.symptoms != null ? List.copyOf(builder.symptoms) : List.of();
        this.relevantObservationIds     = builder.relevantObservationIds != null ?
            List.copyOf(builder.relevantObservationIds) : List.of();
        this.conditions                 = builder.conditions;
        this.description                = builder.description;
        this.sourceFactIds              = builder.sourceFactIds != null ? List.copyOf(builder.sourceFactIds) : List.of();
        this.sourcePassageIds           = builder.sourcePassageIds != null ? List.copyOf(builder.sourcePassageIds) : List.of();
    }

    public String getEntryId()                    { return entryId; }
    public String getFaultName()                  { return faultName; }
    public String getFaultCode()                  { return faultCode; }
    public String getVehicleApplicabilityEntryId() { return vehicleApplicabilityEntryId; }
    public List<String> getSymptoms()             { return symptoms; }
    public List<String> getRelevantObservationIds() { return relevantObservationIds; }
    public String getConditions()                 { return conditions; }
    public String getDescription()                { return description; }
    public List<String> getSourceFactIds()        { return sourceFactIds; }
    public List<String> getSourcePassageIds()     { return sourcePassageIds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String entryId;
        private String faultName;
        private String faultCode;
        private String vehicleApplicabilityEntryId;
        private List<String> symptoms;
        private List<String> relevantObservationIds;
        private String conditions;
        private String description;
        private List<String> sourceFactIds;
        private List<String> sourcePassageIds;

        public Builder entryId(String v)                     { this.entryId = v; return this; }
        public Builder faultName(String v)                   { this.faultName = v; return this; }
        public Builder faultCode(String v)                   { this.faultCode = v; return this; }
        public Builder vehicleApplicabilityEntryId(String v) { this.vehicleApplicabilityEntryId = v; return this; }
        public Builder symptoms(List<String> v)              { this.symptoms = v; return this; }
        public Builder relevantObservationIds(List<String> v) { this.relevantObservationIds = v; return this; }
        public Builder conditions(String v)                  { this.conditions = v; return this; }
        public Builder description(String v)                 { this.description = v; return this; }
        public Builder sourceFactIds(List<String> v)         { this.sourceFactIds = v; return this; }
        public Builder sourcePassageIds(List<String> v)      { this.sourcePassageIds = v; return this; }

        public FaultKnowledgeEntry build() { return new FaultKnowledgeEntry(this); }
    }
}
