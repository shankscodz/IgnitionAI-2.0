package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
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
    public String getUnits() { return "rpm_variance"; }

    @Override
    public String getFormula() { return "variance(engine_rpm)"; }

    @Override
    public String getWindow() { return "10s_regime"; }

    @Override
    public String getFeatureVersion() { return "1.1"; }

    @Override
    public FeatureValue calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        if (ts == null || context.getOperatingConditions().getOperatingRegime() != OperatingRegime.WARM_IDLE) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        List<SensorReading> readings = buffer.getReadings("engine_rpm", ts, 10000L); // 10s window
        if (readings.size() < 10) { // require enough samples for a valid variance
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        double sum = 0;
        for (SensorReading r : readings) sum += r.getValue();
        double mean = sum / readings.size();

        double sqDiffSum = 0;
        for (SensorReading r : readings) {
            sqDiffSum += Math.pow(r.getValue() - mean, 2);
        }
        double variance = sqDiffSum / readings.size();

        return new FeatureValue(getFeatureId(), variance, FeatureStatus.AVAILABLE, List.of("MULTI-OBS-" + ts), ts, getUnits(), getFeatureVersion(), "samples=" + readings.size());
    }
}
