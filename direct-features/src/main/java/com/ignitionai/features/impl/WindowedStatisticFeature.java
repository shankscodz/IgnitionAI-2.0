package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.context.VehicleContext;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class WindowedStatisticFeature implements Feature {
    
    @Override
    public String getFeatureId() { return "coolant_mean_60s"; }
    
    @Override
    public String getName() { return "Coolant Temperature 60s Mean"; }
    
    @Override
    public List<String> getInputs() { return List.of("engine_coolant_temperature"); }
    
    @Override
    public String getUnits() { return "degC"; }
    
    @Override
    public String getFormula() { return "mean(engine_coolant_temperature)"; }
    
    @Override
    public String getWindow() { return "60s"; }
    
    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public FeatureValue calculate(Map<String, Double> inputObservations, Map<String, String> observationIds, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (!inputObservations.containsKey("engine_coolant_temperature")) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }
        
        Double temp = inputObservations.get("engine_coolant_temperature");
        if (temp == null) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.UNAVAILABLE, getFeatureVersion(), ts);
        }
        
        // Mocking window mean calculation for MVP
        Double meanTemp = temp; 
        
        String obsId = observationIds.get("engine_coolant_temperature");
        List<String> obsIdList = obsId != null ? List.of(obsId) : Collections.emptyList();
        
        return new FeatureValue(getFeatureId(), meanTemp, FeatureStatus.AVAILABLE, obsIdList, ts, getUnits(), getFeatureVersion(), "valid");
    }
}
