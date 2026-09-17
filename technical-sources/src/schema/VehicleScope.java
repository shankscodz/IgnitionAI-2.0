package com.ignitionai.technicalsources.schema;

import java.util.List;
import java.util.Objects;

/**
 * Structured vehicle applicability scope for a {@link TechnicalSource}.
 *
 * Authority and applicability are recorded separately.
 * A credible document for the wrong engine must not govern the current vehicle.
 *
 * All fields default to null (unknown), never to a universal "applies to all" assumption.
 */
public final class VehicleScope {

    /**
     * OEM / manufacturer name (e.g. "Volkswagen", "Audi").
     * Null if not specified or unknown.
     */
    private final String manufacturer;

    /**
     * Vehicle model series (e.g. "Golf Mk7", "A4 B9").
     * Null if not specified.
     */
    private final List<String> modelSeries;

    /**
     * Engine family code (e.g. "EA888 Gen3", "EA211").
     * Null if not specified.
     */
    private final List<String> engineFamilies;

    /**
     * Fuel types covered (e.g. "petrol", "diesel", "hybrid").
     * Null if not specified.
     */
    private final List<String> fuelTypes;

    /**
     * Model year range. Both values may be null if the document doesn't specify years.
     */
    private final Integer modelYearFrom;
    private final Integer modelYearTo;

    /**
     * Market / region scope (e.g. "EU", "UK", "US").
     * Null if not restricted to a specific region.
     */
    private final List<String> markets;

    /**
     * Free-text applicability note for edge cases not captured by structured fields.
     */
    private final String applicabilityNote;

    private VehicleScope(Builder builder) {
        this.manufacturer     = builder.manufacturer;
        this.modelSeries      = builder.modelSeries != null ? List.copyOf(builder.modelSeries) : null;
        this.engineFamilies   = builder.engineFamilies != null ? List.copyOf(builder.engineFamilies) : null;
        this.fuelTypes        = builder.fuelTypes != null ? List.copyOf(builder.fuelTypes) : null;
        this.modelYearFrom    = builder.modelYearFrom;
        this.modelYearTo      = builder.modelYearTo;
        this.markets          = builder.markets != null ? List.copyOf(builder.markets) : null;
        this.applicabilityNote = builder.applicabilityNote;
    }

    public String getManufacturer()          { return manufacturer; }
    public List<String> getModelSeries()     { return modelSeries; }
    public List<String> getEngineFamilies()  { return engineFamilies; }
    public List<String> getFuelTypes()       { return fuelTypes; }
    public Integer getModelYearFrom()        { return modelYearFrom; }
    public Integer getModelYearTo()          { return modelYearTo; }
    public List<String> getMarkets()         { return markets; }
    public String getApplicabilityNote()     { return applicabilityNote; }

    /**
     * Returns true only when this scope has at least one structured field populated.
     * A completely null scope means applicability is undetermined — NOT universal.
     */
    public boolean hasDeterminedScope() {
        return manufacturer != null || modelSeries != null || engineFamilies != null ||
               fuelTypes != null || modelYearFrom != null;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String manufacturer;
        private List<String> modelSeries;
        private List<String> engineFamilies;
        private List<String> fuelTypes;
        private Integer modelYearFrom;
        private Integer modelYearTo;
        private List<String> markets;
        private String applicabilityNote;

        public Builder manufacturer(String manufacturer)           { this.manufacturer = manufacturer; return this; }
        public Builder modelSeries(List<String> modelSeries)       { this.modelSeries = modelSeries; return this; }
        public Builder engineFamilies(List<String> engineFamilies) { this.engineFamilies = engineFamilies; return this; }
        public Builder fuelTypes(List<String> fuelTypes)           { this.fuelTypes = fuelTypes; return this; }
        public Builder modelYearFrom(Integer from)                 { this.modelYearFrom = from; return this; }
        public Builder modelYearTo(Integer to)                     { this.modelYearTo = to; return this; }
        public Builder markets(List<String> markets)               { this.markets = markets; return this; }
        public Builder applicabilityNote(String note)              { this.applicabilityNote = note; return this; }

        public VehicleScope build() { return new VehicleScope(this); }
    }
}
