package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;

import java.util.List;

public class CoolantWarmupRateFeature implements Feature {

    @Override
    public String getFeatureId() { return "coolant_warmup_rate"; }

    @Override
    public String getName() { return "Coolant Warmup Rate (Slope)"; }

    @Override
    public List<String> getInputs() { return List.of("engine_coolant_temperature"); }

    @Override
    public String getUnits() { return "degC/min"; }

    @Override
    public String getFormula() { return "slope(engine_coolant_temperature)"; }

    @Override
    public String getWindow() { return "60s"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        List<SensorReading> readings = buffer.getReadings("engine_coolant_temperature", ts, 60000L); // 60s
        if (readings.size() < 5) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        // Calculate simple linear regression slope
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = readings.size();
        
        long baseTs = readings.get(0).getTimestampMs();

        for (SensorReading r : readings) {
            double x = (r.getTimestampMs() - baseTs) / 60000.0; // Minutes
            double y = r.getValue();
            sumX += x;
            sumY += y;
            sumXY += (x * y);
            sumX2 += (x * x);
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        double slope = ((n * sumXY) - (sumX * sumY)) / denominator;

        return new FeatureValue(getFeatureId(), slope, FeatureStatus.AVAILABLE, List.of("MULTI-OBS-" + ts), ts, getUnits(), getFeatureVersion(), "samples=" + n);
    }
}
