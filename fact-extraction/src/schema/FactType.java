package com.ignitionai.factextraction.schema;

/**
 * Type of knowledge captured in a {@link CandidateFact}.
 *
 * Each fact type carries different required and optional fields.
 * The extractor must populate the appropriate structured fields
 * for the declared type — not store type-mismatched data.
 */
public enum FactType {

    /**
     * A configuration constraint or compatibility rule for a vehicle.
     * Example: "EA888 Gen3 requires 5W-30 fully synthetic oil."
     */
    VEHICLE_APPLICABILITY,

    /**
     * A relationship between a symptom and a possible fault.
     * Example: "White smoke at cold start may indicate coolant entering combustion."
     */
    SYMPTOM_FAULT,

    /**
     * A precondition that must be true before a diagnostic action is valid.
     * Example: "Engine must be at operating temperature (>80°C) before fuel trim check."
     */
    DIAGNOSTIC_PRECONDITION,

    /**
     * A diagnostic or repair action that can be performed.
     * Example: "Replace oxygen sensor Bank 1, Sensor 1."
     */
    ACTION,

    /**
     * A tool or equipment required for a diagnostic or repair action.
     * Example: "VAG-COM VCDS required for adaptation reset."
     */
    TOOL_REQUIREMENT,

    /**
     * A numeric limit or range for a measured or computed quantity.
     * Always carries unit, operating conditions and vehicle scope.
     * Example: "Coolant temperature at operating condition: 80–105°C."
     */
    THRESHOLD,

    /**
     * An expected sensor reading or system state under specified conditions.
     * Example: "Long-term fuel trim Bank 1 at idle: within ±10%."
     */
    EXPECTED_OBSERVATION
}
