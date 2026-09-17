package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.context.VehicleContext;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class SpeedMpsFeature implements Feature {
    
    @Override
    public String getFeatureId() { return "speed_mps"; }
    
    @Override
    public String getName() { return "Vehicle Speed (m/s)"; }
    
    @Override
    public List<String> getInputs() { return List.of("vehicle_speed_kph"); }
    
    @Override
    public String getUnits() { return "m/s"; }
    
    @Override
    public String getFormula() { return "vehicle_speed_kph / 3.6"; }
    
    @Override
    public String getWindow() { return "instantaneous"; }
    
    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public FeatureValue calculate(Map<String, Double> inputObservations, Map<String, String> observationIds, VehicleContext context) {
        if (!inputObservations.containsKey("vehicle_speed_kph")) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA);
        }
        
        Double speedKph = inputObservations.get("vehicle_speed_kph");
        if (speedKph == null) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.UNAVAILABLE);
        }
        
        Double speedMps = speedKph / 3.6;
        String obsId = observationIds.get("vehicle_speed_kph");
        List<String> obsIdList = obsId != null ? List.of(obsId) : Collections.emptyList();
        
        return new FeatureValue(getFeatureId(), speedMps, FeatureStatus.AVAILABLE, obsIdList);
    }
}
