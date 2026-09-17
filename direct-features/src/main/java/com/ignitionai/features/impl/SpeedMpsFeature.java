package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

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
    public String getFeatureVersion() { return "1.3"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No timestamp");

        List<SensorReading> readings = buffer.getReadings("vehicle_speed_kph", ts, 1000L);
        if (readings.isEmpty()) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No readings");
        }
        
        SensorReading latest = readings.get(readings.size() - 1);
        Double speedMps = latest.getValue() / 3.6;
        
        return new AnalyticalObservation(getFeatureId(), speedMps, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("OBS-SPEED-" + latest.getTimestampMs()), cv, getFeatureVersion(), "uncertainty unavailable", "direct calculation");
    }
}
