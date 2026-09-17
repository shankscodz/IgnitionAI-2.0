package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.context.VehicleContext;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class TimestampSecondsFeature implements Feature {
    
    @Override
    public String getFeatureId() { return "timestamp_seconds"; }
    
    @Override
    public String getName() { return "Timestamp (seconds)"; }
    
    @Override
    public List<String> getInputs() { return List.of("monotonic_ms"); }
    
    @Override
    public String getUnits() { return "s"; }
    
    @Override
    public String getFormula() { return "monotonic_ms / 1000.0"; }
    
    @Override
    public String getWindow() { return "instantaneous"; }
    
    @Override
    public String getFeatureVersion() { return "1.1"; }

    @Override
    public FeatureValue calculate(Map<String, Double> inputObservations, Map<String, String> observationIds, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (!inputObservations.containsKey("monotonic_ms")) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }
        
        Double monotonicMs = inputObservations.get("monotonic_ms");
        if (monotonicMs == null) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.UNAVAILABLE, getFeatureVersion(), ts);
        }
        
        Double tsSeconds = monotonicMs / 1000.0;
        String obsId = observationIds.get("monotonic_ms");
        List<String> obsIdList = obsId != null ? List.of(obsId) : Collections.emptyList();
        
        return new FeatureValue(getFeatureId(), tsSeconds, FeatureStatus.AVAILABLE, obsIdList, ts, getUnits(), getFeatureVersion(), "valid");
    }
}
