package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

public class MissingDataFractionFeature implements Feature {

    @Override
    public String getFeatureId() { return "missing_data_fraction"; }

    @Override
    public String getName() { return "Missing Data Fraction"; }

    @Override
    public List<String> getInputs() { return List.of("engine_rpm"); }

    @Override
    public String getUnits() { return "fraction"; }

    @Override
    public String getFormula() { return "1.0 - (count(engine_rpm) / expected_count)"; }

    @Override
    public String getWindow() { return "60s"; }

    @Override
    public String getFeatureVersion() { return "1.1"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No context timestamp");

        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 60000L); // 60s
        
        int expectedSamples = 60; // Assume 1Hz
        int actualSamples = Math.min(readings.size(), expectedSamples);
        
        double missingFraction = 1.0 - ((double) actualSamples / expectedSamples);

        return new AnalyticalObservation(getFeatureId(), missingFraction, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
