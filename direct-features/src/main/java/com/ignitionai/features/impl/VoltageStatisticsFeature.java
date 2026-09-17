package com.ignitionai.features.impl;

import com.ignitionai.features.Feature;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;
import com.ignitionai.features.SensorReading;
import com.ignitionai.features.WindowBuffer;
import com.ignitionai.context.VehicleContext;

import java.util.List;

public class VoltageStatisticsFeature implements Feature {

    @Override
    public String getFeatureId() { return "control_module_voltage_min"; }

    @Override
    public String getName() { return "Control Module Voltage Minimum"; }

    @Override
    public List<String> getInputs() { return List.of("control_module_voltage"); }

    @Override
    public String getUnits() { return "V"; }

    @Override
    public String getFormula() { return "min(control_module_voltage)"; }

    @Override
    public String getWindow() { return "60s"; }

    @Override
    public String getFeatureVersion() { return "1.0"; }

    @Override
    public AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context) {
        Long ts = context != null ? context.getContextTimestampMs() : null;
        String cv = context != null ? context.getContextVersion() : null;
        if (ts == null) return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "No timestamp");

        List<SensorReading> readings = buffer.getReadings("control_module_voltage", ts, 60000L);
        if (readings.size() < 5) {
            return AnalyticalObservation.unavailable(getFeatureId(), QualityState.INSUFFICIENT_DATA, getFeatureVersion(), ts, "Not enough samples");
        }

        double minVal = Double.MAX_VALUE;
        for (SensorReading r : readings) {
            if (r.getValue() < minVal) minVal = r.getValue();
        }

        return new AnalyticalObservation(getFeatureId(), minVal, getUnits(), ts, QualityState.AVAILABLE, 
                List.of("MULTI-OBS-" + ts), cv, getFeatureVersion(), "uncertainty unavailable", "samples=" + readings.size());
    }
}
