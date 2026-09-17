package com.ignitionai.features;

import com.ignitionai.context.VehicleContext;
import java.util.List;

public interface Feature {
    String getFeatureId();
    String getName();
    List<String> getInputs();
    String getUnits();
    String getFormula();
    String getWindow();
    String getFeatureVersion();

    FeatureValue calculate(WindowBuffer buffer, VehicleContext context);
}
