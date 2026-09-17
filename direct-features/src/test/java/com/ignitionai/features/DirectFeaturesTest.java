package com.ignitionai.features;

import com.ignitionai.features.impl.SpeedMpsFeature;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.context.VehicleContextManager;
import com.ignitionai.obd.AnalyticalObservation;

public class DirectFeaturesTest {
    public static void main(String[] args) {
        System.out.println("=== direct-features Tests ===");
        
        testFeatureCalculation();
        testMissingInputFallback();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testFeatureCalculation() {
        SpeedMpsFeature speedFeature = new SpeedMpsFeature();
        WindowBuffer buffer = new WindowBuffer();
        buffer.addReading(new SensorReading("vehicle_speed_kph", 36.0, 1000L, "kph"));
        
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx = manager.initializeContext("V1", 1000L);
        
        AnalyticalObservation val = speedFeature.calculate(buffer, ctx);
        
        assert val.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE;
        assert Math.abs(val.getValue() - 10.0) < 0.001;
        assert val.getUnit().equals("m/s");
        System.out.println("  PASS  testFeatureCalculation");
    }

    private static void testMissingInputFallback() {
        SpeedMpsFeature speedFeature = new SpeedMpsFeature();
        WindowBuffer buffer = new WindowBuffer(); // missing vehicle_speed_kph
        
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx = manager.initializeContext("V1", 1000L);
        
        AnalyticalObservation val = speedFeature.calculate(buffer, ctx);
        
        assert val.getQualityState() == AnalyticalObservation.QualityState.INSUFFICIENT_DATA;
        assert val.getValue() == null;
        System.out.println("  PASS  testMissingInputFallback");
    }
}
