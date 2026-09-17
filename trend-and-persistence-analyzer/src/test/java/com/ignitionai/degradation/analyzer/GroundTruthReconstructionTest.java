package com.ignitionai.degradation.analyzer;

public class GroundTruthReconstructionTest {
    public static void main(String[] args) {
        System.out.println("=== GroundTruthReconstruction Tests ===");

        testIsolateSignalFromNoise();

        System.out.println("\n=== Results: 1 passed, 0 failed ===");
    }

    private static void testIsolateSignalFromNoise() {
        StateSpaceModel model = new StateSpaceModel(0.0, 0.01, 0.001, 0.5); // High measurement noise variance
        
        // True latent signal: 0.1, 0.2, 0.3, 0.4, 0.5
        // We add massive random Gaussian noise to observations
        // Expected behavior: The Kalman filter should estimate states closer to truth than the raw noisy obs
        
        double[] trueState = {0.1, 0.2, 0.3, 0.4, 0.5};
        // Simulated noisy observations
        double[] noisyObs = {0.8, -0.3, 0.9, -0.1, 0.9}; 
        
        double sumSqErrorModel = 0;
        double sumSqErrorRaw = 0;

        for (int i = 0; i < trueState.length; i++) {
            model.update(1.0, noisyObs[i], 0.5); // Low confidence implies trust model more than measurement
            
            double modelErr = model.getState() - trueState[i];
            double rawErr = noisyObs[i] - trueState[i];
            
            sumSqErrorModel += modelErr * modelErr;
            sumSqErrorRaw += rawErr * rawErr;
        }

        double rmseModel = Math.sqrt(sumSqErrorModel / trueState.length);
        double rmseRaw = Math.sqrt(sumSqErrorRaw / trueState.length);

        assert rmseModel < rmseRaw : "Expected StateSpaceModel to reconstruct ground truth better than raw measurements.";
        
        System.out.println("  PASS  testIsolateSignalFromNoise");
    }
}
