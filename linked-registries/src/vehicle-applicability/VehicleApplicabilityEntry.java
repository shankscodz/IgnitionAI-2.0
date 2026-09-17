package com.ignitionai.linkedregistries.schema;

import java.util.List;
import java.util.Objects;

/**
 * A published entry in the Vehicle Applicability sub-registry.
 *
 * Records configuration constraints, identifiers and compatibility rules for a vehicle.
 * All entries are pinned to a {@link RegistryRelease} and carry evidence links back
 * to the source passages and facts that produced them.
 *
 * An entry is only published when a fact has reached REVIEWED status via human/technician sign-off.
 */
public final class VehicleApplicabilityEntry {

    /** Stable unique ID for this registry entry. */
    private final String entryId;

    /** Human-readable name of the vehicle configuration (e.g. "VW EA888 Gen3 Petrol"). */
    private final String configurationName;

    /** The manufacturer (e.g. "Volkswagen", "Audi"). */
    private final String manufacturer;

    /** Engine family code (e.g. "EA888 Gen3"). Null if not applicable. */
    private final String engineFamily;

    /** Fuel type (e.g. "petrol", "diesel"). Null if not restricted. */
    private final String fuelType;

    /** Model year range start. Null if not restricted. */
    private final Integer modelYearFrom;

    /** Model year range end. Null if not restricted. */
    private final Integer modelYearTo;

    /** Market / region (e.g. "EU", "UK"). Null if not restricted. */
    private final List<String> markets;

    /** Compatibility rules and constraints in human-readable form. */
    private final String compatibilityNotes;

    /** The factId(s) of the REVIEWED facts that produced this entry. */
    private final List<String> sourceFactIds;

    /** The passageId(s) that supported the facts. For direct provenance verification. */
    private final List<String> sourcePassageIds;

    private VehicleApplicabilityEntry(Builder builder) {
        this.entryId              = Objects.requireNonNull(builder.entryId, "entryId is required");
        this.configurationName    = Objects.requireNonNull(builder.configurationName, "configurationName is required");
        this.manufacturer         = builder.manufacturer;
        this.engineFamily         = builder.engineFamily;
        this.fuelType             = builder.fuelType;
        this.modelYearFrom        = builder.modelYearFrom;
        this.modelYearTo          = builder.modelYearTo;
        this.markets              = builder.markets != null ? List.copyOf(builder.markets) : null;
        this.compatibilityNotes   = builder.compatibilityNotes;
        this.sourceFactIds        = builder.sourceFactIds != null ? List.copyOf(builder.sourceFactIds) : List.of();
        this.sourcePassageIds     = builder.sourcePassageIds != null ? List.copyOf(builder.sourcePassageIds) : List.of();
    }

    public String getEntryId()              { return entryId; }
    public String getConfigurationName()    { return configurationName; }
    public String getManufacturer()         { return manufacturer; }
    public String getEngineFamily()         { return engineFamily; }
    public String getFuelType()             { return fuelType; }
    public Integer getModelYearFrom()       { return modelYearFrom; }
    public Integer getModelYearTo()         { return modelYearTo; }
    public List<String> getMarkets()        { return markets; }
    public String getCompatibilityNotes()   { return compatibilityNotes; }
    public List<String> getSourceFactIds()  { return sourceFactIds; }
    public List<String> getSourcePassageIds() { return sourcePassageIds; }

    public enum CompatibilityResult {
        APPLICABLE,
        INCOMPATIBLE,
        INSUFFICIENT_CONTEXT
    }

    /**
     * Checks compatibility with the given vehicle context.
     * Returns APPLICABLE if all constraints are met.
     * Returns INCOMPATIBLE if any constraint is violated.
     * Returns INSUFFICIENT_CONTEXT if a constraint exists but the target context is missing that parameter.
     */
    public CompatibilityResult checkCompatibility(String targetManufacturer, String targetEngineFamily,
                                     String targetFuelType, Integer targetModelYear) {
        if (manufacturer != null) {
            if (targetManufacturer == null || targetManufacturer.isBlank()) return CompatibilityResult.INSUFFICIENT_CONTEXT;
            if (!manufacturer.equalsIgnoreCase(targetManufacturer)) return CompatibilityResult.INCOMPATIBLE;
        }
        if (engineFamily != null) {
            if (targetEngineFamily == null || targetEngineFamily.isBlank()) return CompatibilityResult.INSUFFICIENT_CONTEXT;
            if (!engineFamily.equalsIgnoreCase(targetEngineFamily)) return CompatibilityResult.INCOMPATIBLE;
        }
        if (fuelType != null) {
            if (targetFuelType == null || targetFuelType.isBlank()) return CompatibilityResult.INSUFFICIENT_CONTEXT;
            if (!fuelType.equalsIgnoreCase(targetFuelType)) return CompatibilityResult.INCOMPATIBLE;
        }
        if (modelYearFrom != null || modelYearTo != null) {
            if (targetModelYear == null) return CompatibilityResult.INSUFFICIENT_CONTEXT;
            if (modelYearFrom != null && targetModelYear < modelYearFrom) return CompatibilityResult.INCOMPATIBLE;
            if (modelYearTo != null && targetModelYear > modelYearTo) return CompatibilityResult.INCOMPATIBLE;
        }
        return CompatibilityResult.APPLICABLE;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String entryId;
        private String configurationName;
        private String manufacturer;
        private String engineFamily;
        private String fuelType;
        private Integer modelYearFrom;
        private Integer modelYearTo;
        private List<String> markets;
        private String compatibilityNotes;
        private List<String> sourceFactIds;
        private List<String> sourcePassageIds;

        public Builder entryId(String v)              { this.entryId = v; return this; }
        public Builder configurationName(String v)    { this.configurationName = v; return this; }
        public Builder manufacturer(String v)         { this.manufacturer = v; return this; }
        public Builder engineFamily(String v)         { this.engineFamily = v; return this; }
        public Builder fuelType(String v)             { this.fuelType = v; return this; }
        public Builder modelYearFrom(Integer v)       { this.modelYearFrom = v; return this; }
        public Builder modelYearTo(Integer v)         { this.modelYearTo = v; return this; }
        public Builder markets(List<String> v)        { this.markets = v; return this; }
        public Builder compatibilityNotes(String v)   { this.compatibilityNotes = v; return this; }
        public Builder sourceFactIds(List<String> v)  { this.sourceFactIds = v; return this; }
        public Builder sourcePassageIds(List<String> v) { this.sourcePassageIds = v; return this; }

        public VehicleApplicabilityEntry build() { return new VehicleApplicabilityEntry(this); }
    }
}
