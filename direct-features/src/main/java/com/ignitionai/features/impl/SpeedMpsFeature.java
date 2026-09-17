package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;
import java.util.stream.Collectors;

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
    public String getFeatureVersion() { return "1.2"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), null);

        List<SensorReading> readings = buffer.getReadings("vehicle_speed_kph", ts, 1000L); // 1s tolerance for instantaneous
        if (readings.isEmpty()) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }
        
        SensorReading latest = readings.get(readings.size() - 1);
        Double speedMps = latest.getValue() / 3.6;
        
        return new FeatureValue(getFeatureId(), speedMps, FeatureStatus.AVAILABLE, List.of("OBS-SPEED-" + latest.getTimestampMs()), ts, getUnits(), getFeatureVersion(), "valid");
    }
}
