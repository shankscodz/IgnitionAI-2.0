package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

public class FuelTrimSummaryFeature implements Feature {

    @Override
    public String getFeatureId() { return "fuel_trim_summary_bank1"; }

    @Override
    public String getName() { return "Fuel Trim Summary Bank 1"; }

    @Override
    public List<String> getInputs() { return List.of("long_term_fuel_trim_bank1", "short_term_fuel_trim_bank1"); }

    @Override
    public String getUnits() { return "%"; }

    @Override
    public String getFormula() { return "mean(long_term_fuel_trim_bank1 + short_term_fuel_trim_bank1)"; }

    @Override
    public String getWindow() { return "60s"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No context timestamp");

        List<SensorReading> ltft = buffer.getReadings("long_term_fuel_trim_bank1", ts, 60000L);
        List<SensorReading> stft = buffer.getReadings("short_term_fuel_trim_bank1", ts, 60000L);
        
        if (ltft.size() < 5 || stft.size() < 5) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        }

        // For MVP, just average the means if they are loosely synchronized
        double ltftSum = 0;
        for (SensorReading r : ltft) ltftSum += r.getValue();
        
        double stftSum = 0;
        for (SensorReading r : stft) stftSum += r.getValue();
        
        double totalTrimMean = (ltftSum / ltft.size()) + (stftSum / stft.size());

        return new AnalyticalObservation(getFeatureId(), totalTrimMean, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + ltft.size());
    }
}
