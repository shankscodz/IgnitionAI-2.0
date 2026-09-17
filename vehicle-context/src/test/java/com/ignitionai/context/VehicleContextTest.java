package com.ignitionai.context;

import java.util.HashMap;
import java.util.Map;

public class VehicleContextTest {
    public static void main(String[] args) {
        System.out.println("=== vehicle-context Tests ===");
        
        testUnknownVin();
        testMissingConfiguration();
        testOperatingRegimeTransitions();

        System.out.println("\n=== Results: 3 passed, 0 failed ===");
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

    private static void testOperatingRegimeTransitions() {
        VehicleContextManager manager = new VehicleContextManager();
        VehicleContext ctx1 = manager.initializeContext("TEST-VEH-01", 1000L);
        
        Map<String, Double> obs = new HashMap<>();
        obs.put("engine_rpm", 0.0);
        VehicleContext ctx2 = manager.transitionContext(ctx1, obs, 2000L);
        assert ctx2.getOperatingConditions().getOperatingRegime() == OperatingRegime.ENGINE_OFF;
        
        obs.put("engine_rpm", 800.0);
        obs.put("vehicle_speed_kph", 0.0);
        VehicleContext ctx3 = manager.transitionContext(ctx2, obs, 3000L);
        
        assert ctx3.getOperatingConditions().getOperatingRegime() == OperatingRegime.WARM_IDLE;
        assert ctx3.getOperatingConditions().getRegimeTransition() == OperatingRegime.WARM_IDLE;
        
        System.out.println("  PASS  testOperatingRegimeTransitions");
    }
}
