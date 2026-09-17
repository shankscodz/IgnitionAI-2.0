package com.ignitionai.linkedregistries.schema;

import java.util.List;
import java.util.Objects;

/**
 * A published entry in the Observation Registry sub-registry.
 *
 * Records a measured or computed quantity with its definition, unit, operating conditions
 * and supported limits. All downstream modules reference observations by their observationId.
 *
 * The Observation Registry is the authoritative source for signal definitions used
 * by Direct Features, Virtual Sensors, Expected Behaviour and Anomaly Monitoring.
 */
public final class ObservationEntry {

    /** Stable unique observation ID (e.g. "OBS-COOLANT-TEMP", "OBS-LTFT-B1"). */
    private final String observationId;

    /** Human-readable name (e.g. "Coolant Temperature", "Long-term Fuel Trim Bank 1"). */
    private final String name;

    /**
     * Whether this is a directly measured OBD signal ("MEASURED") or
     * a computed/derived quantity ("COMPUTED"). Never ambiguous.
     */
    private final String measurementType;

    /** Physical unit of the observation (e.g. "°C", "%", "bar"). Must be in the known unit set. */
    private final String unit;

    /** Definition of what this observation represents physically. */
    private final String definition;

    /** Operating conditions under which this observation is valid/meaningful. */
    private final String operatingConditions;

    /**
     * Normal operating limit lower bound. Null if no lower limit is defined.
     * This is a reference value, not a fault threshold — those are in separate THRESHOLD facts.
     */
    private final Double normalLimitLower;

    /**
     * Normal operating limit upper bound. Null if no upper limit is defined.
     */
    private final Double normalLimitUpper;

    /** Vehicle applicability entry ID this observation applies to. Cross-registry reference. */
    private final String vehicleApplicabilityEntryId;

    /** OBD/CAN PID or DID if this is a directly measured signal. Null for computed quantities. */
    private final String pidOrDid;

    /** The factId(s) of the REVIEWED facts that produced this entry. */
    private final List<String> sourceFactIds;

    /** The passageId(s) that supported the facts. */
    private final List<String> sourcePassageIds;

    private ObservationEntry(Builder builder) {
        this.observationId              = Objects.requireNonNull(builder.observationId, "observationId is required");
        this.name                       = Objects.requireNonNull(builder.name, "name is required");
        this.measurementType            = Objects.requireNonNull(builder.measurementType, "measurementType is required");
        this.unit                       = Objects.requireNonNull(builder.unit, "unit is required");
        this.definition                 = builder.definition;
        this.operatingConditions        = builder.operatingConditions;
        this.normalLimitLower           = builder.normalLimitLower;
        this.normalLimitUpper           = builder.normalLimitUpper;
        this.vehicleApplicabilityEntryId = builder.vehicleApplicabilityEntryId;
        this.pidOrDid                   = builder.pidOrDid;
        this.sourceFactIds              = builder.sourceFactIds != null ? List.copyOf(builder.sourceFactIds) : List.of();
        this.sourcePassageIds           = builder.sourcePassageIds != null ? List.copyOf(builder.sourcePassageIds) : List.of();
    }

    public String getObservationId()              { return observationId; }
    public String getName()                       { return name; }
    public String getMeasurementType()            { return measurementType; }
    public String getUnit()                       { return unit; }
    public String getDefinition()                 { return definition; }
    public String getOperatingConditions()        { return operatingConditions; }
    public Double getNormalLimitLower()           { return normalLimitLower; }
    public Double getNormalLimitUpper()           { return normalLimitUpper; }
    public String getVehicleApplicabilityEntryId() { return vehicleApplicabilityEntryId; }
    public String getPidOrDid()                   { return pidOrDid; }
    public List<String> getSourceFactIds()        { return sourceFactIds; }
    public List<String> getSourcePassageIds()     { return sourcePassageIds; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String observationId;
        private String name;
        private String measurementType;
        private String unit;
        private String definition;
        private String operatingConditions;
        private Double normalLimitLower;
        private Double normalLimitUpper;
        private String vehicleApplicabilityEntryId;
        private String pidOrDid;
        private List<String> sourceFactIds;
        private List<String> sourcePassageIds;

        public Builder observationId(String v)              { this.observationId = v; return this; }
        public Builder name(String v)                       { this.name = v; return this; }
        public Builder measurementType(String v)            { this.measurementType = v; return this; }
        public Builder unit(String v)                       { this.unit = v; return this; }
        public Builder definition(String v)                 { this.definition = v; return this; }
        public Builder operatingConditions(String v)        { this.operatingConditions = v; return this; }
        public Builder normalLimitLower(Double v)           { this.normalLimitLower = v; return this; }
        public Builder normalLimitUpper(Double v)           { this.normalLimitUpper = v; return this; }
        public Builder vehicleApplicabilityEntryId(String v) { this.vehicleApplicabilityEntryId = v; return this; }
        public Builder pidOrDid(String v)                   { this.pidOrDid = v; return this; }
        public Builder sourceFactIds(List<String> v)        { this.sourceFactIds = v; return this; }
        public Builder sourcePassageIds(List<String> v)     { this.sourcePassageIds = v; return this; }

        public ObservationEntry build() { return new ObservationEntry(this); }
    }

    @Override
    public String toString() {
        return "ObservationEntry{id='" + observationId + "', name='" + name +
               "', unit='" + unit + "', type=" + measurementType + "}";
    }
}
