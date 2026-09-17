package com.ignitionai.linkedregistries.schema;

import java.time.Instant;
import java.util.*;

/**
 * A versioned snapshot of all four linked registries at a point in time.
 *
 * Every analytical session must pin to one {@code RegistryRelease} by its {@code releaseId}.
 * Knowledge edits cannot silently rewrite an old report because the release is immutable once created.
 *
 * Draft content is kept separate from the published release. Only REVIEWED facts
 * may be included in a published release; CHECKED facts are draft-eligible only.
 *
 * Cross-registry references within a release are guaranteed to be internally consistent:
 * if a FaultKnowledgeEntry references an observationId, that observationId exists in
 * the same release's Observation Registry.
 */
public final class RegistryRelease {

    /**
     * Stable, monotonically increasing release identifier.
     * Format: "REL-YYYYMMDD-{sequence}" (e.g. "REL-20260917-001").
     */
    private final String releaseId;

    /** Human-readable description of this release (e.g. "Initial EA888 Gen3 knowledge pack"). */
    private final String description;

    /** Time at which this release was created and sealed. Immutable after creation. */
    private final Instant createdAt;

    /**
     * The vehicle family this release covers.
     * A release is scoped to a specific vehicle family to prevent cross-contamination.
     */
    private final String vehicleFamily;

    /** Whether this is a DRAFT (CHECKED facts) or PUBLISHED (REVIEWED facts) release. */
    private final ReleaseStatus status;

    /** Reviewer identity for PUBLISHED releases. Null for DRAFT. */
    private final String reviewerIdentity;

    /** Review time for PUBLISHED releases. Null for DRAFT. */
    private final Instant reviewTime;

    /** Review decision for PUBLISHED releases. Null for DRAFT. */
    private final String reviewDecision;

    /** All Vehicle Applicability entries in this release. Immutable. */
    private final List<VehicleApplicabilityEntry> vehicleApplicabilityEntries;

    /** All Fault Knowledge Model entries in this release. Immutable. */
    private final List<FaultKnowledgeEntry> faultKnowledgeEntries;

    /** All Action Registry entries in this release. Immutable. */
    private final List<ActionEntry> actionEntries;

    /** All Observation Registry entries in this release. Immutable. */
    private final List<ObservationEntry> observationEntries;

    public enum ReleaseStatus {
        /** Draft release — contains CHECKED facts. Not for production use. */
        DRAFT,
        /** Published release — contains only REVIEWED/approved facts. */
        PUBLISHED
    }

    private RegistryRelease(Builder builder) {
        this.releaseId                  = Objects.requireNonNull(builder.releaseId, "releaseId is required");
        this.description                = builder.description;
        this.createdAt                  = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        this.vehicleFamily              = builder.vehicleFamily;
        this.status                     = Objects.requireNonNull(builder.status, "status is required");
        this.reviewerIdentity           = builder.reviewerIdentity;
        this.reviewTime                 = builder.reviewTime;
        this.reviewDecision             = builder.reviewDecision;
        this.vehicleApplicabilityEntries = builder.vehicleApplicabilityEntries != null ?
            List.copyOf(builder.vehicleApplicabilityEntries) : List.of();
        this.faultKnowledgeEntries      = builder.faultKnowledgeEntries != null ?
            List.copyOf(builder.faultKnowledgeEntries) : List.of();
        this.actionEntries              = builder.actionEntries != null ?
            List.copyOf(builder.actionEntries) : List.of();
        this.observationEntries         = builder.observationEntries != null ?
            List.copyOf(builder.observationEntries) : List.of();
    }

    public String getReleaseId()          { return releaseId; }
    public String getDescription()        { return description; }
    public Instant getCreatedAt()         { return createdAt; }
    public String getVehicleFamily()      { return vehicleFamily; }
    public ReleaseStatus getStatus()      { return status; }
    public String getReviewerIdentity()   { return reviewerIdentity; }
    public Instant getReviewTime()        { return reviewTime; }
    public String getReviewDecision()     { return reviewDecision; }
    public List<VehicleApplicabilityEntry> getVehicleApplicabilityEntries() { return vehicleApplicabilityEntries; }
    public List<FaultKnowledgeEntry> getFaultKnowledgeEntries()             { return faultKnowledgeEntries; }
    public List<ActionEntry> getActionEntries()                             { return actionEntries; }
    public List<ObservationEntry> getObservationEntries()                   { return observationEntries; }

    public int getTotalEntryCount() {
        return vehicleApplicabilityEntries.size() + faultKnowledgeEntries.size() +
               actionEntries.size() + observationEntries.size();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String releaseId;
        private String description;
        private Instant createdAt;
        private String vehicleFamily;
        private ReleaseStatus status;
        private String reviewerIdentity;
        private Instant reviewTime;
        private String reviewDecision;
        private List<VehicleApplicabilityEntry> vehicleApplicabilityEntries;
        private List<FaultKnowledgeEntry> faultKnowledgeEntries;
        private List<ActionEntry> actionEntries;
        private List<ObservationEntry> observationEntries;

        public Builder releaseId(String v)                      { this.releaseId = v; return this; }
        public Builder description(String v)                    { this.description = v; return this; }
        public Builder createdAt(Instant v)                     { this.createdAt = v; return this; }
        public Builder vehicleFamily(String v)                  { this.vehicleFamily = v; return this; }
        public Builder status(ReleaseStatus v)                  { this.status = v; return this; }
        public Builder reviewerIdentity(String v)               { this.reviewerIdentity = v; return this; }
        public Builder reviewTime(Instant v)                    { this.reviewTime = v; return this; }
        public Builder reviewDecision(String v)                 { this.reviewDecision = v; return this; }
        public Builder vehicleApplicabilityEntries(List<VehicleApplicabilityEntry> v) { this.vehicleApplicabilityEntries = v; return this; }
        public Builder faultKnowledgeEntries(List<FaultKnowledgeEntry> v)             { this.faultKnowledgeEntries = v; return this; }
        public Builder actionEntries(List<ActionEntry> v)       { this.actionEntries = v; return this; }
        public Builder observationEntries(List<ObservationEntry> v) { this.observationEntries = v; return this; }

        public RegistryRelease build() { return new RegistryRelease(this); }
    }

    @Override
    public String toString() {
        return "RegistryRelease{releaseId='" + releaseId + "', status=" + status +
               ", entries=" + getTotalEntryCount() + ", vehicleFamily='" + vehicleFamily + "'}";
    }
}
