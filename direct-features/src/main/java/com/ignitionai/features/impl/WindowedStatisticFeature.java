package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
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
    public String getFeatureVersion() { return "1.1"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), null);

        List<SensorReading> readings = buffer.getReadings("engine_coolant_temperature", ts, 60000L);
        if (readings.size() < 5) { // minSampleCount
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }
        
        double sum = 0;
        for (SensorReading r : readings) {
            sum += r.getValue();
        }
        double mean = sum / readings.size();
        
        return new FeatureValue(getFeatureId(), mean, FeatureStatus.AVAILABLE, List.of("MULTI-OBS-" + ts), ts, getUnits(), getFeatureVersion(), "samples=" + readings.size());
    }
}
