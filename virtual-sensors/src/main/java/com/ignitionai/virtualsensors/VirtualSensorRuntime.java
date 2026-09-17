package com.ignitionai.virtualsensors;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.features.SensorReading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VirtualSensorRuntime {
    private Map<String, SensorDefinition> registeredSensors = new HashMap<>();
    private Map<String, VirtualSensorPlugin> plugins = new HashMap<>();

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

    public void registerPlugin(VirtualSensorPlugin plugin) {
        plugins.put(plugin.getSensorId(), plugin);
    }

    private void validateDAG() {
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

    public SensorOutput evaluate(String sensorId, Map<String, SensorReading> featureValues, VehicleContext context) {
        SensorDefinition def = registeredSensors.get(sensorId);
        if (def == null) return SensorOutput.unsupported(sensorId, "Sensor not found in registry");

        // Validate preconditions
        if (def.getOperatingPreconditions() != null) {
            String requiredRegime = def.getOperatingPreconditions().get("engine_running");
            if ("true".equals(requiredRegime) && (context == null || context.getOperatingConditions() == null || !Boolean.TRUE.equals(context.getOperatingConditions().getEngineRunningState()))) {
                return SensorOutput.notApplicable(sensorId, "Precondition failed: engine must be running");
            }
        }

        // Validate dependencies
        Map<String, Double> inputDoubles = new HashMap<>();
        if (def.getInputSignalIds() != null) {
            for (String dep : def.getInputSignalIds()) {
                SensorReading reading = featureValues.get(dep);
                if (reading == null) {
                    return SensorOutput.missingInputs(sensorId, "Missing required input: " + dep);
                }
                // Basic dimensional check (skipped full unit conversion for MVP, just exact match or assumed correct if missing units in definition)
                inputDoubles.put(dep, reading.getValue());
            }
        }

        try {
            // Plugin evaluation
            if ("plugin".equals(def.getImplementationType())) {
                VirtualSensorPlugin plugin = plugins.get(sensorId);
                if (plugin == null) return SensorOutput.unsupported(sensorId, "Missing plugin implementation");
                return plugin.execute(inputDoubles, context);
            }

            // Formula evaluation
            String formula = def.getFormulaOrModelReference();
            if (formula != null) {
                // simple diff support
                if (formula.startsWith("diff(")) {
                    String[] parts = formula.replace("diff(", "").replace(")", "").split(",");
                    if (parts.length == 2) {
                        Double v1 = inputDoubles.get(parts[0].trim());
                        Double v2 = inputDoubles.get(parts[1].trim());
                        if (v1 != null && v2 != null) {
                            return SensorOutput.available(sensorId, v1 - v2);
                        }
                    }
                } 
                // Basic Math Parse logic for demo
                else if (formula.contains("/")) {
                    String[] parts = formula.split("/");
                    Double v1 = inputDoubles.get(parts[0].trim());
                    Double v2 = inputDoubles.get(parts[1].trim());
                    if (v1 != null && v2 != null && v2 != 0) {
                        return SensorOutput.available(sensorId, v1 / v2);
                    }
                }
            }
            return SensorOutput.notApplicable(sensorId, "Formula could not be evaluated: " + formula);
        } catch (Exception e) {
            return SensorOutput.error(sensorId, "Evaluation failed: " + e.getMessage());
        }
    }

    public Map<String, SensorDefinition> getRegisteredSensors() {
        return registeredSensors;
    }
}
