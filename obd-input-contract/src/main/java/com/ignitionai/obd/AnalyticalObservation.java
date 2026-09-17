package com.ignitionai.obd;

import java.util.List;
import java.util.Collections;

public class AnalyticalObservation {
    public enum QualityState {
        AVAILABLE,
        INSUFFICIENT_DATA,
        MISSING_INPUTS,
        UNSUPPORTED,
        NOT_APPLICABLE,
        EVALUATION_ERROR
    }

    private final String observationId;
    private final Double value;
    private final String unit;
    private final Long timestampMs;
    private final QualityState qualityState;
    private final List<String> sourceObservationReferences;
    private final String contextVersion;
    private final String definitionVersion;
    private final String uncertainty;
    private final String provenance;

    public AnalyticalObservation(String observationId, Double value, String unit, Long timestampMs, 
                                 QualityState qualityState, List<String> sourceObservationReferences, 
                                 String contextVersion, String definitionVersion, 
                                 String uncertainty, String provenance) {
        this.observationId = observationId;
        this.value = value;
        this.unit = unit;
        this.timestampMs = timestampMs;
        this.qualityState = qualityState != null ? qualityState : QualityState.EVALUATION_ERROR;
        this.sourceObservationReferences = sourceObservationReferences != null ? Collections.unmodifiableList(sourceObservationReferences) : Collections.emptyList();
        this.contextVersion = contextVersion;
        this.definitionVersion = definitionVersion;
        this.uncertainty = uncertainty != null ? uncertainty : "uncertainty unavailable";
        this.provenance = provenance;
    }

    public static AnalyticalObservation unavailable(String observationId, QualityState state, String definitionVersion, Long timestampMs, String reason) {
        return new AnalyticalObservation(observationId, null, null, timestampMs, state, List.of(), null, definitionVersion, "uncertainty unavailable", reason);
    }

    public String getObservationId() { return observationId; }
    public Double getValue() { return value; }
    public String getUnit() { return unit; }
    public Long getTimestampMs() { return timestampMs; }
    public QualityState getQualityState() { return qualityState; }
    public List<String> getSourceObservationReferences() { return sourceObservationReferences; }
    public String getContextVersion() { return contextVersion; }
    public String getDefinitionVersion() { return definitionVersion; }
    public String getUncertainty() { return uncertainty; }
    public String getProvenance() { return provenance; }
}
