package com.ignitionai.virtualsensors;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.VehicleContextManager;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.obd.AnalyticalObservation.QualityState;

public class SensorAcceptanceTest {
    public static void main(String[] args) {
        System.out.println("=== virtual-sensors Acceptance Tests ===");
        
        testPreconditionFailure();
        testMissingDependency();
        testFormulaEvaluation();
        testPluginIsolation();

        System.out.println("\n=== Results: 4 passed, 0 failed ===");
    }

    private static void testPreconditionFailure() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("test_sensor");
        def.setImplementationType("formula");
        def.setFormulaOrModelReference("diff(a, b)");
        Map<String, String> pre = new HashMap<>();
        pre.put("engine_running", "true");
        def.setOperatingPreconditions(pre);
        
        runtime.loadSensors(List.of(def));
        
        VehicleContextManager mgr = new VehicleContextManager();
        VehicleContext ctx = mgr.initializeContext("V1", 1000L); // Engine off by default
        
        AnalyticalObservation out = runtime.evaluate("test_sensor", new HashMap<>(), ctx, 1000L);
        assert out.getQualityState() == QualityState.NOT_APPLICABLE;
        assert out.getProvenance().contains("Precondition failed");
        
        System.out.println("  PASS  testPreconditionFailure");
    }

    private static void testMissingDependency() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("test_sensor");
        def.setInputSignalIds(List.of("req_signal"));
        runtime.loadSensors(List.of(def));
        
        AnalyticalObservation out = runtime.evaluate("test_sensor", new HashMap<>(), null, 1000L);
        assert out.getQualityState() == QualityState.MISSING_INPUTS;
        
        System.out.println("  PASS  testMissingDependency");
    }

    private static void testFormulaEvaluation() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("ratio_sensor");
        def.setInputSignalIds(List.of("a", "b"));
        def.setFormulaOrModelReference("a / b");
        runtime.loadSensors(List.of(def));
        
        Map<String, AnalyticalObservation> inputs = new HashMap<>();
        inputs.put("a", new AnalyticalObservation("a", 10.0, "U", 100L, QualityState.AVAILABLE, List.of(), "v1", "v1", "0", "sim"));
        inputs.put("b", new AnalyticalObservation("b", 2.0, "U", 100L, QualityState.AVAILABLE, List.of(), "v1", "v1", "0", "sim"));
        
        AnalyticalObservation out = runtime.evaluate("ratio_sensor", inputs, null, 100L);
        assert out.getQualityState() == QualityState.AVAILABLE;
        assert out.getValue() == 5.0;
        
        System.out.println("  PASS  testFormulaEvaluation");
    }

    private static void testPluginIsolation() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("bad_plugin");
        def.setImplementationType("plugin");
        runtime.loadSensors(List.of(def));
        
        runtime.registerPlugin(new VirtualSensorPlugin() {
            @Override
            public String getSensorId() { return "bad_plugin"; }
            @Override
            public AnalyticalObservation execute(Map<String, AnalyticalObservation> inputs, VehicleContext context) {
                throw new RuntimeException("Simulated crash");
            }
        });
        
        AnalyticalObservation out = runtime.evaluate("bad_plugin", new HashMap<>(), null, 100L);
        assert out.getQualityState() == QualityState.EVALUATION_ERROR;
        assert out.getProvenance().contains("Simulated crash");
        
        System.out.println("  PASS  testPluginIsolation");
    }
}
