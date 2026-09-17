package com.ignitionai.linkedregistries.schema;

import java.util.List;
import java.util.Objects;

/**
 * A published entry in the Action Registry sub-registry.
 *
 * Records a diagnostic or repair action with its preconditions, required tools,
 * observable outcomes and sourced time estimates.
 */
public final class ActionEntry {

    /** Stable unique ID for this action entry. */
    private final String entryId;

    /** Human-readable name of the action (e.g. "Replace Coolant Thermostat"). */
    private final String actionName;

    /** Category of action: DIAGNOSTIC, REPAIR, CALIBRATION, INSPECTION, RESET. */
    private final String category;

    /** Vehicle applicability entry ID this action applies to. Cross-registry reference. */
    private final String vehicleApplicabilityEntryId;

    /** Preconditions that must be true before this action is valid (e.g. "engine at operating temp"). */
    private final List<String> preconditions;

    /** Required tools by name (e.g. "VCDS", "Torque wrench 20–80 Nm"). */
    private final List<String> requiredTools;

    /**
     * Observation IDs that should be checked before/after the action.
     * Cross-registry reference to the Observation Registry.
     */
    private final List<String> relatedObservationIds;

    /** Expected observable outcomes after successful completion of the action. */
    private final List<String> expectedOutcomes;

    /**
     * Estimated time from source document (e.g. "0.5h", "1–1.5h").
     * Null if the source did not provide an estimate.
     * Must not be invented — only populated when source text supports it.
     */
    private final String sourcedTimeEstimate;

    /** The factId(s) of the REVIEWED facts that produced this entry. */
    private final List<String> sourceFactIds;

    /** The passageId(s) that supported the facts. */
    private final List<String> sourcePassageIds;

    private ActionEntry(Builder builder) {
        this.entryId                    = Objects.requireNonNull(builder.entryId, "entryId is required");
        this.actionName                 = Objects.requireNonNull(builder.actionName, "actionName is required");
        this.category                   = builder.category;
        this.vehicleApplicabilityEntryId = builder.vehicleApplicabilityEntryId;
        this.preconditions              = builder.preconditions != null ? List.copyOf(builder.preconditions) : List.of();
        this.requiredTools              = builder.requiredTools != null ? List.copyOf(builder.requiredTools) : List.of();
        this.relatedObservationIds      = builder.relatedObservationIds != null ? List.copyOf(builder.relatedObservationIds) : List.of();
        this.expectedOutcomes           = builder.expectedOutcomes != null ? List.copyOf(builder.expectedOutcomes) : List.of();
        this.sourcedTimeEstimate        = builder.sourcedTimeEstimate;
        this.sourceFactIds              = builder.sourceFactIds != null ? List.copyOf(builder.sourceFactIds) : List.of();
        this.sourcePassageIds           = builder.sourcePassageIds != null ? List.copyOf(builder.sourcePassageIds) : List.of();
    }

    public String getEntryId()                    { return entryId; }
    public String getActionName()                 { return actionName; }
    public String getCategory()                   { return category; }
    public String getVehicleApplicabilityEntryId() { return vehicleApplicabilityEntryId; }
    public List<String> getPreconditions()        { return preconditions; }
    public List<String> getRequiredTools()        { return requiredTools; }
    public List<String> getRelatedObservationIds() { return relatedObservationIds; }
    public List<String> getExpectedOutcomes()     { return expectedOutcomes; }
    public String getSourcedTimeEstimate()        { return sourcedTimeEstimate; }
    public List<String> getSourceFactIds()        { return sourceFactIds; }
    public List<String> getSourcePassageIds()     { return sourcePassageIds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String entryId;
        private String actionName;
        private String category;
        private String vehicleApplicabilityEntryId;
        private List<String> preconditions;
        private List<String> requiredTools;
        private List<String> relatedObservationIds;
        private List<String> expectedOutcomes;
        private String sourcedTimeEstimate;
        private List<String> sourceFactIds;
        private List<String> sourcePassageIds;

        public Builder entryId(String v)                     { this.entryId = v; return this; }
        public Builder actionName(String v)                  { this.actionName = v; return this; }
        public Builder category(String v)                    { this.category = v; return this; }
        public Builder vehicleApplicabilityEntryId(String v) { this.vehicleApplicabilityEntryId = v; return this; }
        public Builder preconditions(List<String> v)         { this.preconditions = v; return this; }
        public Builder requiredTools(List<String> v)         { this.requiredTools = v; return this; }
        public Builder relatedObservationIds(List<String> v) { this.relatedObservationIds = v; return this; }
        public Builder expectedOutcomes(List<String> v)      { this.expectedOutcomes = v; return this; }
        public Builder sourcedTimeEstimate(String v)         { this.sourcedTimeEstimate = v; return this; }
        public Builder sourceFactIds(List<String> v)         { this.sourceFactIds = v; return this; }
        public Builder sourcePassageIds(List<String> v)      { this.sourcePassageIds = v; return this; }

        public ActionEntry build() { return new ActionEntry(this); }
    }
}
