package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
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
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);

        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 60000L); // 60s
        
        // Assume expected rate is 1Hz (60 samples)
        int expectedSamples = 60;
        int actualSamples = Math.min(readings.size(), expectedSamples);
        
        double missingFraction = 1.0 - ((double) actualSamples / expectedSamples);

        return new FeatureValue(getFeatureId(), missingFraction, FeatureStatus.AVAILABLE, List.of("MULTI-OBS-" + ts), ts, getUnits(), getFeatureVersion(), "samples=" + readings.size());
    }
}
