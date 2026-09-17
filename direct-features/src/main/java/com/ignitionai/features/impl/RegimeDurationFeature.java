package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.OperatingRegime;

import java.util.List;

public class RegimeDurationFeature implements Feature {

    @Override
    public String getFeatureId() { return "regime_duration_s"; }

    @Override
    public String getName() { return "Regime Duration"; }

    @Override
    public List<String> getInputs() { return List.of(); } // Derived purely from context

    @Override
    public String getUnits() { return "s"; }

    @Override
    public String getFormula() { return "context_ts - current_regime_start_ts"; }

    @Override
    public String getWindow() { return "instantaneous"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null || context.getOperatingConditions().getOperatingRegime() == OperatingRegime.UNKNOWN) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Unknown regime or missing context");
        }

        // Simplification for MVP: find the latest transition to the current regime
        long regimeStart = 0L;
        if (context.getOperatingConditions().getCurrentRegimeStartTimeMs() != null) {
            regimeStart = context.getOperatingConditions().getCurrentRegimeStartTimeMs();
        } else {
            regimeStart = ts;
        }

        double duration = (ts - regimeStart) / 1000.0;

        return new AnalyticalObservation(getFeatureId(), duration, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("CTX-REGIME-" + cv), cv, getFeatureVersion(), "uncertainty unavailable", "regime=" + context.getOperatingConditions().getOperatingRegime());
    }
}
