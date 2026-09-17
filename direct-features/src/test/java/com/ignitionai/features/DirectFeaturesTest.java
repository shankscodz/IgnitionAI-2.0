package com.ignitionai.features;

import com.ignitionai.features.impl.SpeedMpsFeature;
import com.ignitionai.features.impl.TimestampSecondsFeature;
import com.ignitionai.context.VehicleContext;

import java.util.HashMap;
import java.util.Map;

public class DirectFeaturesTest {
    public static void main(String[] args) {
        System.out.println("=== direct-features Tests ===");
        
        testFeatureCalculation();
        testMissingInputFallback();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testFeatureCalculation() {
        SpeedMpsFeature speedFeature = new SpeedMpsFeature();
        Map<String, Double> inputs = new HashMap<>();
        inputs.put("vehicle_speed_kph", 36.0);
        
        Map<String, String> obsIds = new HashMap<>();
        obsIds.put("vehicle_speed_kph", "OBS-01");
        
        VehicleContext ctx = new VehicleContext("V1");
        
        FeatureValue val = speedFeature.calculate(inputs, obsIds, ctx);
        
        assert val.getStatus() == FeatureStatus.AVAILABLE;
        assert Math.abs(val.getValue() - 10.0) < 0.001;
        assert val.getSourceObservationIds().contains("OBS-01");
        System.out.println("  PASS  testFeatureCalculation");
    }

    private static void testMissingInputFallback() {
        SpeedMpsFeature speedFeature = new SpeedMpsFeature();
        Map<String, Double> inputs = new HashMap<>(); // missing vehicle_speed_kph
        
        VehicleContext ctx = new VehicleContext("V1");
        
        FeatureValue val = speedFeature.calculate(inputs, new HashMap<>(), ctx);
        
        assert val.getStatus() == FeatureStatus.INSUFFICIENT_DATA;
        assert val.getValue() == null;
        System.out.println("  PASS  testMissingInputFallback");
    }
}
