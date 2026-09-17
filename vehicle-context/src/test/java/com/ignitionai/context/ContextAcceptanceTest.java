package com.ignitionai.context;

import java.util.HashMap;
import java.util.Map;

public class ContextAcceptanceTest {
    public static void main(String[] args) {
        System.out.println("=== vehicle-context Acceptance Tests ===");
        
        testUnknownVin();
        testMissingConfiguration();
        testOperatingRegimeTransitionsAndHistory();
        testOut_of_orderTimestamp();

        System.out.println("\n=== Results: 4 passed, 0 failed ===");
    }

    private static void testUnknownVin() {
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx = manager.initializeContext("TEST-VEH-01", 1000L);
        assert ctx.getIdentity().getIdentityStatus() == Identity.IdentityStatus.UNKNOWN;
        assert ctx.getIdentity().getVin() == null;
        System.out.println("  PASS  testUnknownVin");
    }

    private static void testMissingConfiguration() {
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx = manager.initializeContext("TEST-VEH-01", 1000L);
        assert ctx.getConfiguration().getState() == DataState.UNKNOWN;
        assert ctx.getConfiguration().getEngineCode() == null;
        System.out.println("  PASS  testMissingConfiguration");
    }

    private static void testOperatingRegimeTransitionsAndHistory() {
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx1 = manager.initializeContext("TEST-VEH-01", 1000L);
        
        Map<String, Double> obs = new HashMap<>();
        obs.put("engine_rpm", 0.0);
        VehicleContext ctx2 = manager.transitionContext(ctx1, obs, 2000L);
        assert ctx2.getOperatingConditions().getOperatingRegime() == OperatingRegime.ENGINE_OFF;
        assert ctx2.getOperatingConditions().getTransitionHistory().size() == 1;
        
        obs.put("engine_rpm", 800.0);
        obs.put("vehicle_speed_kph", 0.0);
        VehicleContext ctx3 = manager.transitionContext(ctx2, obs, 3000L);
        
        assert ctx3.getOperatingConditions().getOperatingRegime() == OperatingRegime.WARM_IDLE;
        assert ctx3.getOperatingConditions().getRegimeTransition() == OperatingRegime.WARM_IDLE;
        assert ctx3.getOperatingConditions().getTransitionHistory().size() == 2;
        assert ctx3.getOperatingConditions().getTransitionHistory().get(0) == OperatingRegime.ENGINE_OFF;
        assert ctx3.getOperatingConditions().getTransitionHistory().get(1) == OperatingRegime.WARM_IDLE;
        
        System.out.println("  PASS  testOperatingRegimeTransitionsAndHistory");
    }
    
    private static void testOut_of_orderTimestamp() {
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx1 = manager.initializeContext("TEST-VEH-01", 5000L);
        Map<String, Double> obs = new HashMap<>();
        try {
            manager.transitionContext(ctx1, obs, 4000L);
            throw new AssertionError("Should have rejected out-of-order timestamp");
        } catch (IllegalArgumentException e) {
            System.out.println("  PASS  testOut_of_orderTimestamp");
        }
    }
}
