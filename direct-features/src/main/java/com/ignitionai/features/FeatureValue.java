package com.ignitionai.features;

import java.util.List;

public class FeatureValue {
    private String featureId;
    private Double value;
    private FeatureStatus status;
    private List<String> sourceObservationIds;

    public FeatureValue(String featureId, Double value, FeatureStatus status, List<String> sourceObservationIds) {
        this.featureId = featureId;
        this.value = value;
        this.status = status;
        this.sourceObservationIds = sourceObservationIds;
    }

    public static FeatureValue unavailable(String featureId, FeatureStatus status) {
        return new FeatureValue(featureId, null, status, List.of());
    }

    public String getFeatureId() { return featureId; }
    public Double getValue() { return value; }
    public FeatureStatus getStatus() { return status; }
    public List<String> getSourceObservationIds() { return sourceObservationIds; }
}
