package com.ignitionai.features;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.AnalyticalObservation;
import java.util.List;

public interface Feature {
    String getFeatureId();
    String getName();
    List<String> getInputs();
    String getUnits();
    String getFormula();
    String getWindow();
    String getFeatureVersion();

    AnalyticalObservation calculate(WindowBuffer buffer, VehicleContext context);
}
