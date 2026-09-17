package com.ignitionai.virtualsensors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VirtualSensorRuntime {
    private Map<String, SensorDefinition> registeredSensors = new HashMap<>();

    public void loadSensors(List<SensorDefinition> definitions) {
        for (SensorDefinition def : definitions) {
            if (registeredSensors.containsKey(def.getSensorId())) {
                System.err.println("Duplicate sensor ID detected: " + def.getSensorId());
                continue;
            }
            registeredSensors.put(def.getSensorId(), def);
        }
        
        validateDAG();
    }

    private void validateDAG() {
        // Detect cycles
        for (String id : registeredSensors.keySet()) {
            Set<String> visited = new HashSet<>();
            Set<String> recStack = new HashSet<>();
            if (isCyclic(id, visited, recStack)) {
                System.err.println("Cycle detected in dependency graph involving: " + id);
            }
        }
    }

    private boolean isCyclic(String current, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(current)) return true;
        if (visited.contains(current)) return false;

        visited.add(current);
        recStack.add(current);

        SensorDefinition def = registeredSensors.get(current);
        if (def != null && def.getInputFeatureIds() != null) {
            for (String dep : def.getInputFeatureIds()) {
                if (registeredSensors.containsKey(dep)) {
                    if (isCyclic(dep, visited, recStack)) return true;
                }
            }
        }
        
        recStack.remove(current);
        return false;
    }

    public SensorOutput evaluate(String sensorId, Map<String, Double> featureValues) {
        SensorDefinition def = registeredSensors.get(sensorId);
        if (def == null) return SensorOutput.unsupported(sensorId, "Sensor not found in registry");

        // Validate dependencies
        if (def.getInputSignalIds() != null) {
            for (String dep : def.getInputSignalIds()) {
                if (!featureValues.containsKey(dep)) {
                    return SensorOutput.missingInputs(sensorId, "Missing required input: " + dep);
                }
            }
        }

        try {
            String formula = def.getFormulaOrModelReference();
            if (formula != null && formula.startsWith("diff(")) {
                String[] parts = formula.replace("diff(", "").replace(")", "").split(",");
                if (parts.length == 2) {
                    Double v1 = featureValues.get(parts[0].trim());
                    Double v2 = featureValues.get(parts[1].trim());
                    if (v1 != null && v2 != null) {
                        return SensorOutput.available(sensorId, v1 - v2);
                    }
                }
            }
            return SensorOutput.notApplicable(sensorId, "Formula could not be evaluated: " + formula);
        } catch (Exception e) {
            // Failure containment - plugin/formula isolation
            return SensorOutput.error(sensorId, "Evaluation failed: " + e.getMessage());
        }
    }

    public Map<String, SensorDefinition> getRegisteredSensors() {
        return registeredSensors;
    }
}
