package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
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
    public String getFeatureVersion() { return "1.1"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No context timestamp");
        }

        List<SensorReading> readings = buffer.getReadings("engine_coolant_temperature", ts, 60000L); // 60s
        if (readings.size() < 5) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        }

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
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Zero denominator");
        }

        double slope = ((n * sumXY) - (sumX * sumY)) / denominator;

        return new AnalyticalObservation(getFeatureId(), slope, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + n);
    }
}
