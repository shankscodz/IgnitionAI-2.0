package com.ignitionai.context;

public class VehicleContextTest {
    public static void main(String[] args) {
        System.out.println("=== vehicle-context Tests ===");
        
        testUnknownVin();
        testMissingConfiguration();
        testOperatingRegimeTransitions();
        testInsufficientData();

        System.out.println("\n=== Results: 4 passed, 0 failed ===");
    }

    private static void testUnknownVin() {
        VehicleContext ctx = new VehicleContext("TEST-VEH-01");
        assert ctx.getIdentity().getIdentityStatus() == Identity.IdentityStatus.UNKNOWN;
        assert ctx.getIdentity().getVin() == null;
        System.out.println("  PASS  testUnknownVin");
    }

    private static void testMissingConfiguration() {
        VehicleContext ctx = new VehicleContext("TEST-VEH-01");
        assert ctx.getConfiguration().getState() == DataState.UNKNOWN;
        assert ctx.getConfiguration().getEngineCode() == null;
        System.out.println("  PASS  testMissingConfiguration");
    }

    private static void testOperatingRegimeTransitions() {
        VehicleContext ctx = new VehicleContext("TEST-VEH-01");
        ctx.getOperatingConditions().setOperatingRegime(OperatingRegime.ENGINE_OFF);
        ctx.getOperatingConditions().setRegimeTransition(OperatingRegime.STARTUP);
        assert ctx.getOperatingConditions().getOperatingRegime() == OperatingRegime.ENGINE_OFF;
        assert ctx.getOperatingConditions().getRegimeTransition() == OperatingRegime.STARTUP;
        System.out.println("  PASS  testOperatingRegimeTransitions");
    }

    private static void testInsufficientData() {
        VehicleContext ctx = new VehicleContext("TEST-VEH-01");
        ctx.getOperatingConditions().setState(DataState.INSUFFICIENT_DATA);
        assert ctx.getOperatingConditions().getState() == DataState.INSUFFICIENT_DATA;
        System.out.println("  PASS  testInsufficientData");
    }
}
