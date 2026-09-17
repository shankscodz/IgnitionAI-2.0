package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.features.FeatureStatus;
import com.ignitionai.features.FeatureValue;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class IdleRpmStabilityFeature implements Feature {

    @Override
    public String getFeatureId() { return "idle_rpm_stability"; }

    @Override
    public String getName() { return "Idle RPM Stability"; }

    @Override
    public List<String> getInputs() { return List.of("engine_rpm"); }

    @Override
    public String getUnits() { return "rpm_variance"; }

    @Override
    public String getFormula() { return "variance(engine_rpm)"; }

    @Override
    public String getWindow() { return "10s_regime"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public FeatureValue calculate(Map<String, Double> inputObservations, Map<String, String> observationIds, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        
        if (context == null || context.getOperatingConditions().getOperatingRegime() != OperatingRegime.WARM_IDLE) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        if (!inputObservations.containsKey("engine_rpm")) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.INSUFFICIENT_DATA, getFeatureVersion(), ts);
        }

        // Mock variance for demonstration
        Double rpm = inputObservations.get("engine_rpm");
        if (rpm == null) {
            return FeatureValue.unavailable(getFeatureId(), FeatureStatus.UNAVAILABLE, getFeatureVersion(), ts);
        }

        Double stability = rpm * 0.01; // Mock calculation
        
        String obsId = observationIds.get("engine_rpm");
        List<String> obsIdList = obsId != null ? List.of(obsId) : Collections.emptyList();

        return new FeatureValue(getFeatureId(), stability, FeatureStatus.AVAILABLE, obsIdList, ts, getUnits(), getFeatureVersion(), "computed_over_idle_window");
    }
}
