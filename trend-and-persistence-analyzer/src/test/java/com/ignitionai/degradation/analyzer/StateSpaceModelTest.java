package com.ignitionai.degradation.analyzer;

public class StateSpaceModelTest {
    public static void main(String[] args) {
        System.out.println("=== StateSpaceModel Tests ===");

        testModelConvergence();
        testModelReset();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testModelConvergence() {
        StateSpaceModel model = new StateSpaceModel(0.1, 0.01, 0.001, 0.1);
        
        // Feed measurements that linearly increase
        model.update(1.0, 0.2, 0.9);
        model.update(1.0, 0.3, 0.9);
        model.update(1.0, 0.4, 0.9);
        model.update(1.0, 0.5, 0.9);
        
        assert model.getState() > 0.4 : "Expected state to converge near 0.5";
        assert model.getVelocity() > 0.05 : "Expected positive velocity";
        assert model.getUncertainty() < 0.5 : "Expected uncertainty to decrease over time";
        
        System.out.println("  PASS  testModelConvergence");
    }

    private static void testModelReset() {
        StateSpaceModel model = new StateSpaceModel(0.1, 0.01, 0.001, 0.1);
        model.update(1.0, 0.5, 0.9);
        model.update(1.0, 0.6, 0.9);
        
        double u1 = model.getUncertainty();
        
        // Simulate repair or calibration reset
        model.resetUncertainty();
        double u2 = model.getUncertainty();
        
        assert u2 > u1 : "Expected uncertainty to jump back up after reset";
        assert model.getVelocity() == 0.0 : "Expected velocity to reset";
        
        System.out.println("  PASS  testModelReset");
    }
}
