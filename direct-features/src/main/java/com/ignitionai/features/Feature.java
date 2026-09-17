package com.ignitionai.features;

import java.util.List;
import java.util.Map;
import com.ignitionai.context.VehicleContext;

public interface Feature {
    String getFeatureId();
    String getName();
    List<String> getInputs();
    String getUnits();
    String getFormula();
    String getWindow();
    String getFeatureVersion();
    
    FeatureValue calculate(Map<String, Double> inputObservations, Map<String, String> observationIds, VehicleContext context);
}
