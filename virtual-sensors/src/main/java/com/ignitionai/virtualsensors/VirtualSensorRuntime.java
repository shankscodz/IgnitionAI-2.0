package com.ignitionai.virtualsensors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualSensorRuntime {
    private Map<String, SensorDefinition> registeredSensors = new HashMap<>();

    public void loadSensors(List<SensorDefinition> definitions) {
        for (SensorDefinition def : definitions) {
            if (registeredSensors.containsKey(def.getSensorId())) {
                System.err.println("Duplicate sensor ID detected: " + def.getSensorId());
                continue;
            }
            // validate cycles and missing deps here if needed
            registeredSensors.put(def.getSensorId(), def);
        }
    }

    public Double evaluate(String sensorId, Map<String, Double> featureValues) {
        SensorDefinition def = registeredSensors.get(sensorId);
        if (def == null) return null;

        // MVP: Support simple declarative expression evaluating
        // e.g. "fuel_trim_balance = (long_term_fuel_trim_bank1 - long_term_fuel_trim_bank2)"
        // For mvp we'll do a simple mock/substitution based on the formula
        String formula = def.getFormulaOrModelReference();
        if (formula != null && formula.startsWith("diff(")) {
            String[] parts = formula.replace("diff(", "").replace(")", "").split(",");
            if (parts.length == 2) {
                Double v1 = featureValues.get(parts[0].trim());
                Double v2 = featureValues.get(parts[1].trim());
                if (v1 != null && v2 != null) {
                    return v1 - v2;
                }
            }
        }
        return null;
    }

    public Map<String, SensorDefinition> getRegisteredSensors() {
        return registeredSensors;
    }
}
