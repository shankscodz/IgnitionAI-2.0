package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
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
    public String getFeatureVersion() { return "1.3"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");

        List<SensorReading> readings = buffer.getReadings("monotonic_ms", ts, 1000L);
        if (readings.isEmpty()) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No readings");
        }
        
        SensorReading latest = readings.get(readings.size() - 1);
        Double tsSeconds = latest.getValue() / 1000.0;
        
        return new AnalyticalObservation(getFeatureId(), tsSeconds, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("OBS-TIME-" + latest.getTimestampMs()), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
