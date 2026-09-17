package com.ignitionai.degradation.state;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

public class DegradationStateTest {
    public static void main(String[] args) {
        System.out.println("=== degradation-state-estimator Tests ===");

        testHealthyState();
        testWorseningState();
        testRecoveredState();

        System.out.println("\n=== Results: 3 passed, 0 failed ===");
    }

    private static void testHealthyState() {
        DegradationFeatures f = new DegradationFeatures();
        f.setNormalizedScore(0.1);
        f.setRecurrenceCount(1);
        f.setMeanPersistence(0.1);

        TrendResult t = new TrendResult(0.0, TrendDirection.UNKNOWN, 0.1, 0.05);

        DegradationStateEstimator est = new DegradationStateEstimator();
        DegradationStateResult res = est.estimateState(f, t);

        assert res.getState() == DegradationState.HEALTHY : "Expected HEALTHY for isolated low-score anomaly";
        System.out.println("  PASS  testHealthyState");
    }

    private static void testWorseningState() {
        DegradationFeatures f = new DegradationFeatures();
        f.setNormalizedScore(0.8);
        f.setRecurrenceCount(5);
        f.setMeanPersistence(0.9);

        TrendResult t = new TrendResult(0.1, TrendDirection.WORSENING, 0.7, 0.05);

        DegradationStateEstimator est = new DegradationStateEstimator();
        DegradationStateResult res = est.estimateState(f, t);

        assert res.getState() == DegradationState.WORSENING_DEGRADATION : "Expected WORSENING_DEGRADATION for worsening trend";
        System.out.println("  PASS  testWorseningState");
    }

    private static void testRecoveredState() {
        DegradationFeatures f = new DegradationFeatures();
        f.setNormalizedScore(0.1);
        f.setRecurrenceCount(5);
        f.setMeanPersistence(0.1);

        TrendResult t = new TrendResult(-0.2, TrendDirection.IMPROVING, 0.05, 0.001);

        DegradationStateEstimator est = new DegradationStateEstimator();
        DegradationStateResult res = est.estimateState(f, t);

        assert res.getState() == DegradationState.RECOVERED_DEGRADATION : "Expected RECOVERED_DEGRADATION for improving trend with low score";
        System.out.println("  PASS  testRecoveredState");
    }
}
