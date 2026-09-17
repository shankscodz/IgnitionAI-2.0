package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

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
    public String getFeatureVersion() { return "1.2"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), null);

        List<SensorReading> readings = buffer.getReadings("monotonic_ms", ts, 1000L);
        if (readings.isEmpty()) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }
        
        SensorReading latest = readings.get(readings.size() - 1);
        Double tsSeconds = latest.getValue() / 1000.0;
        
        return new FeatureValue(getFeatureId(), tsSeconds, FeatureStatus.AVAILABLE, List.of("OBS-TIME-" + latest.getTimestampMs()), ts, getUnits(), getFeatureVersion(), "valid");
    }
}
