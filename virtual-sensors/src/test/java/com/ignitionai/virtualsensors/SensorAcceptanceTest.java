package com.ignitionai.virtualsensors;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.VehicleContextManager;
import com.ignitionai.features.SensorReading;

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
        
        SensorOutput out = runtime.evaluate("test_sensor", new HashMap<>(), ctx);
        assert out.getStatus() == SensorOutput.Status.NOT_APPLICABLE;
        assert out.getReason().contains("Precondition failed");
        
        System.out.println("  PASS  testPreconditionFailure");
    }

    private static void testMissingDependency() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("test_sensor");
        def.setInputSignalIds(List.of("req_signal"));
        runtime.loadSensors(List.of(def));
        
        SensorOutput out = runtime.evaluate("test_sensor", new HashMap<>(), null);
        assert out.getStatus() == SensorOutput.Status.MISSING_INPUTS;
        
        System.out.println("  PASS  testMissingDependency");
    }

    private static void testFormulaEvaluation() {
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        SensorDefinition def = new SensorDefinition();
        def.setSensorId("ratio_sensor");
        def.setInputSignalIds(List.of("a", "b"));
        def.setFormulaOrModelReference("a / b");
        runtime.loadSensors(List.of(def));
        
        Map<String, SensorReading> inputs = new HashMap<>();
        inputs.put("a", new SensorReading("a", 10.0, 100L, "U"));
        inputs.put("b", new SensorReading("b", 2.0, 100L, "U"));
        
        SensorOutput out = runtime.evaluate("ratio_sensor", inputs, null);
        assert out.getStatus() == SensorOutput.Status.AVAILABLE;
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
            public SensorOutput execute(Map<String, Double> inputs, VehicleContext context) {
                throw new RuntimeException("Simulated crash");
            }
        });
        
        SensorOutput out = runtime.evaluate("bad_plugin", new HashMap<>(), null);
        assert out.getStatus() == SensorOutput.Status.EVALUATION_ERROR;
        assert out.getReason().contains("Simulated crash");
        
        System.out.println("  PASS  testPluginIsolation");
    }
}
