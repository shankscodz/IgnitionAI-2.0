package com.ignitionai.virtualsensors;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.ignitionai.linkedregistries.schema.ObservationEntry;

public class VirtualSensorsTest {
    public static void main(String[] args) {
        System.out.println("=== virtual-sensors Tests ===");
        
        testPhase3ExitDemonstration();

        System.out.println("\n=== Results: 1 passed, 0 failed ===");
    }

    private static void testPhase3ExitDemonstration() {
        System.out.println("  [Demo] Phase 3 Exit Criteria Execution...");
        
        // 1. Discover manifests at startup (Adding sensor without core code changes)
        ManifestLoader loader = new ManifestLoader();
        File dir = new File("virtual-sensors/src/main/resources/sensors");
        List<SensorDefinition> defs = loader.loadManifests(dir);
        
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        runtime.loadSensors(defs);
        
        // Ensure metadata is enumerated correctly
        SensorDefinition thermalProxy = runtime.getRegisteredSensors().get("thermal_response_proxy");
        assert thermalProxy != null;
        assert thermalProxy.getUnits().equals("degC/s");
        assert thermalProxy.getInputSignalIds().contains("engine_coolant_temperature");
        System.out.println("    -> Discovered and enumerated metadata successfully.");

        // 2. Evaluate with full inputs
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("engine_coolant_temperature", 85.0);
        inputs.put("ambient_air_temperature", 25.0);
        
        SensorOutput outSuccess = runtime.evaluate("thermal_response_proxy", inputs);
        assert outSuccess.getStatus() == SensorOutput.Status.AVAILABLE;
        assert Math.abs(outSuccess.getValue() - 60.0) < 0.001;
        System.out.println("    -> Evaluated successfully with valid inputs.");

        // 3. Remove required input and observe explicit unavailable result
        inputs.remove("ambient_air_temperature");
        SensorOutput outMissing = runtime.evaluate("thermal_response_proxy", inputs);
        assert outMissing.getStatus() == SensorOutput.Status.MISSING_INPUTS;
        assert outMissing.getValue() == null;
        assert outMissing.getReason().contains("ambient_air_temperature");
        System.out.println("    -> Explicit unavailable behavior (MISSING_INPUTS) observed.");

        System.out.println("  PASS  testPhase3ExitDemonstration");
    }
}
