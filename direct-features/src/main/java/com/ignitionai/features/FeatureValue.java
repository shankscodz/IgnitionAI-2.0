package com.ignitionai.features;

import java.util.List;
import java.util.Collections;

public class FeatureValue {
    private final String featureId;
    private final Double value;
    private final FeatureStatus status;
    private final List<String> sourceObservationIds;
    private final Long timestampMs;
    private final String units;
    private final String definitionVersion;
    private final String qualityMetadata;

    public FeatureValue(String featureId, Double value, FeatureStatus status, 
                        List<String> sourceObservationIds, Long timestampMs, 
                        String units, String definitionVersion, String qualityMetadata) {
        this.featureId = featureId;
        this.value = value;
        this.status = status;
        this.sourceObservationIds = sourceObservationIds != null ? Collections.unmodifiableList(sourceObservationIds) : Collections.emptyList();
        this.timestampMs = timestampMs;
        this.units = units;
        this.definitionVersion = definitionVersion;
        this.qualityMetadata = qualityMetadata;
    }

    public static FeatureValue unavailable(String featureId, FeatureStatus status, String definitionVersion, Long timestampMs) {
        return new FeatureValue(featureId, null, status, List.of(), timestampMs, null, definitionVersion, "Missing or invalid inputs");
    }

    public String getFeatureId() { return featureId; }
    public Double getValue() { return value; }
    public FeatureStatus getStatus() { return status; }
    public List<String> getSourceObservationIds() { return sourceObservationIds; }
    public Long getTimestampMs() { return timestampMs; }
    public String getUnits() { return units; }
    public String getDefinitionVersion() { return definitionVersion; }
    public String getQualityMetadata() { return qualityMetadata; }
}
