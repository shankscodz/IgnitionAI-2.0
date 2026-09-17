package com.ignitionai.virtualsensors;

import java.io.File;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.ignitionai.linkedregistries.schema.ObservationEntry;

public class VirtualSensorsTest {
    public static void main(String[] args) {
        System.out.println("=== virtual-sensors Tests ===");
        
        testManifestLoading();
        testSensorEvaluation();
        testPhase1RegistryIntegration();

        System.out.println("\n=== Results: 3 passed, 0 failed ===");
    }

    private static void testManifestLoading() {
        ManifestLoader loader = new ManifestLoader();
        // Point to the resources directory in our layout
        File dir = new File("virtual-sensors/src/main/resources/sensors");
        List<SensorDefinition> defs = loader.loadManifests(dir);
        
        assert defs.size() == 3;
        
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        runtime.loadSensors(defs);
        
        assert runtime.getRegisteredSensors().size() == 3;
        assert runtime.getRegisteredSensors().containsKey("thermal_response_proxy");
        System.out.println("  PASS  testManifestLoading");
    }

    private static void testSensorEvaluation() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("test_sensor");
        def.setFormulaOrModelReference("diff(val1, val2)");
        runtime.loadSensors(List.of(def));
        
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("val1", 100.0);
        inputs.put("val2", 40.0);
        
        Double result = runtime.evaluate("test_sensor", inputs);
        assert result != null;
        assert Math.abs(result - 60.0) < 0.001;
        System.out.println("  PASS  testSensorEvaluation");
    }

    private static void testPhase1RegistryIntegration() {
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("virtual_coolant");
        def.setDisplayName("Virtual Coolant");
        def.setObservationKind("MODEL_DERIVED");
        def.setUnits("degC");
        def.setFormulaOrModelReference("diff(a,b)");
        def.setInputSignalIds(List.of("a", "b"));
        def.setSamplingRequirements("1Hz");

        ObservationEntry entry = ObservationEntry.builder()
            .observationId(def.getSensorId())
            .name(def.getDisplayName())
            .measurementType("COMPUTED") // Legacy, needed by builder if observationKind is explicitly set
            .observationKind(def.getObservationKind())
            .unit(def.getUnits())
            .formulaOrModelReference(def.getFormulaOrModelReference())
            .inputObservationIds(def.getInputSignalIds())
            .samplingRequirements(def.getSamplingRequirements())
            .build();

        assert "MODEL_DERIVED".equals(entry.getObservationKind());
        assert entry.getInputObservationIds().contains("a");
        System.out.println("  PASS  testPhase1RegistryIntegration");
    }
}
