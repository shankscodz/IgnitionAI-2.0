package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

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
    public String getFeatureVersion() { return "1.2"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), null, "No context timestamp");

        List<SensorReading> readings = buffer.getReadings("engine_coolant_temperature", ts, 60000L);
        if (readings.size() < 5) { // minSampleCount
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        }
        
        double sum = 0;
        for (SensorReading r : readings) sum += r.getValue();
        double mean = sum / readings.size();
        
        return new AnalyticalObservation(getFeatureId(), mean, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "std=" + Math.sqrt(mean), "samples=" + readings.size());
    }
}
