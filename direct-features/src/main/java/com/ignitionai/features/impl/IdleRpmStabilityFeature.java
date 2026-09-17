package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;

import java.util.List;

public class IdleRpmStabilityFeature implements Feature {

    @Override
    public String getFeatureId() { return "idle_rpm_stability"; }

    @Override
    public String getName() { return "Idle RPM Stability (Variance)"; }

    @Override
    public List<String> getInputs() { return List.of("engine_rpm"); }

    @Override
    public String getUnits() { return "rpm^2"; }

    @Override
    public String getFormula() { return "variance(engine_rpm)"; }

    @Override
    public String getWindow() { return "10s_regime"; }

    @Override
    public String getFeatureVersion() { return "1.2"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null || context.getOperatingConditions().getOperatingRegime() != OperatingRegime.WARM_IDLE) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not in WARM_IDLE");
        }

        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 10000L); // 10s window
        if (readings.size() < 10) { 
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        }

        double sum = 0;
        for (SensorReading r : readings) sum += r.getValue();
        double mean = sum / readings.size();

        double sqDiffSum = 0;
        for (SensorReading r : readings) {
            sqDiffSum += Math.pow(r.getValue() - mean, 2);
        }
        double variance = sqDiffSum / readings.size();

        return new AnalyticalObservation(getFeatureId(), variance, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
