package com.ignitionai.features;

import com.ignitionai.features.impl.CoolantWarmupRateFeature;
import com.ignitionai.features.impl.IdleRpmStabilityFeature;
import com.ignitionai.features.impl.WindowedStatisticFeature;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.VehicleContextManager;

import java.util.HashMap;
import java.util.Map;

public class FeatureAcceptanceTest {
    public static void main(String[] args) {
        System.out.println("=== direct-features Acceptance Tests ===");
        
        testWindowedMean();
        testIdleRpmStability();
        testCoolantWarmupRate();
        testInsufficientData();

        System.out.println("\n=== Results: 4 passed, 0 failed ===");
    }

    private static void testWindowedMean() {
        WindowBuffer buffer = new WindowBuffer();
        long ts = 100000L;
        // 6 samples
        for (int i = 0; i < 6; i++) {
            buffer.addReading(new SensorReading("engine_coolant_temperature", 80.0 + i, ts + (i * 1000), "degC"));
        }
        
        VehicleContextManager mgr = new VehicleContextManager();
        VehicleContext ctx = mgr.initializeContext("V1", ts + 5000);
        
        WindowedStatisticFeature feature = new WindowedStatisticFeature();
        FeatureValue val = feature.calculate(buffer, ctx);
        
        assert val.getStatus() == FeatureStatus.AVAILABLE;
        assert Math.abs(val.getValue() - 82.5) < 0.001; // Mean of 80,81,82,83,84,85
        System.out.println("  PASS  testWindowedMean");
    }

    private static void testIdleRpmStability() {
        WindowBuffer buffer = new WindowBuffer();
        long ts = 100000L;
        for (int i = 0; i < 10; i++) {
            buffer.addReading(new SensorReading("engine_rpm", 800.0 + (i % 2 == 0 ? 10 : -10), ts + (i * 1000), "rpm"));
        }
        
        VehicleContextManager mgr = new VehicleContextManager();
        VehicleContext ctx1 = mgr.initializeContext("V1", 1000L);
        Map<String, Double> obs = new HashMap<>();
        obs.put("engine_rpm", 800.0);
        obs.put("vehicle_speed_kph", 0.0);
        VehicleContext ctx = mgr.transitionContext(ctx1, obs, ts + 9000); // Forces WARM_IDLE
        
        IdleRpmStabilityFeature feature = new IdleRpmStabilityFeature();
        FeatureValue val = feature.calculate(buffer, ctx);
        
        assert val.getStatus() == FeatureStatus.AVAILABLE;
        assert val.getValue() > 0; // Variance should be exactly 100
        System.out.println("  PASS  testIdleRpmStability");
    }

    private static void testCoolantWarmupRate() {
        WindowBuffer buffer = new WindowBuffer();
        long ts = 100000L;
        // 1 degree per 10 seconds -> 6 degrees per min
        for (int i = 0; i < 6; i++) {
            buffer.addReading(new SensorReading("engine_coolant_temperature", 50.0 + i, ts + (i * 10000), "degC"));
        }
        
        VehicleContextManager mgr = new VehicleContextManager();
        VehicleContext ctx = mgr.initializeContext("V1", ts + 50000);
        
        CoolantWarmupRateFeature feature = new CoolantWarmupRateFeature();
        FeatureValue val = feature.calculate(buffer, ctx);
        
        assert val.getStatus() == FeatureStatus.AVAILABLE;
        assert Math.abs(val.getValue() - 6.0) < 0.001; // Slope: 6 deg/min
        System.out.println("  PASS  testCoolantWarmupRate");
    }

    private static void testInsufficientData() {
        WindowBuffer buffer = new WindowBuffer();
        long ts = 100000L;
        // Only 3 samples, feature requires 5
        for (int i = 0; i < 3; i++) {
            buffer.addReading(new SensorReading("engine_coolant_temperature", 80.0 + i, ts + (i * 1000), "degC"));
        }
        
        VehicleContextManager mgr = new VehicleContextManager();
        VehicleContext ctx = mgr.initializeContext("V1", ts + 5000);
        
        WindowedStatisticFeature feature = new WindowedStatisticFeature();
        FeatureValue val = feature.calculate(buffer, ctx);
        
        assert val.getStatus() == FeatureStatus.INSUFFICIENT_DATA;
        assert val.getValue() == null;
        System.out.println("  PASS  testInsufficientData");
    }
}
