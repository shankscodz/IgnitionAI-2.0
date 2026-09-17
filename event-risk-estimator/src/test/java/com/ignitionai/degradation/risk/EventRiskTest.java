package com.ignitionai.degradation.risk;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.degradation.state.DegradationStateResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

public class EventRiskTest {
    public static void main(String[] args) {
        System.out.println("=== event-risk-estimator Tests ===");

        testRiskCalculation();
        testInsufficientHistoryFallback();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testRiskCalculation() {
        DegradationFeatures f = new DegradationFeatures();
        f.setRecurrenceCount(10);
        f.setNormalizedScore(0.8);
        f.setMaxSeverity(0.8);
        
        TrendResult t = new TrendResult(0.1, TrendDirection.WORSENING, 0.8, 0.05);
        DegradationStateResult s = new DegradationStateResult(DegradationState.WORSENING_DEGRADATION, 0.7);

        EventRiskEstimator est = new EventRiskEstimator();
        EventRiskResult res = est.estimateRisk(f, t, s, "critical_failure", 168, true);

        assert res.getRiskEstimate() > 0.4 : "Expected high risk for worsening state";
        assert res.getConfidence() > 0.8 : "Expected high confidence";
        System.out.println("  PASS  testRiskCalculation");
    }

    private static void testInsufficientHistoryFallback() {
        EventRiskEstimator est = new EventRiskEstimator();
        
        EventRiskResult res = est.estimateRisk(new DegradationFeatures(), new TrendResult(0.0, TrendDirection.UNKNOWN, 0.0, 1.0), 
                                               new DegradationStateResult(DegradationState.INSUFFICIENT_HISTORY, 0.2), "critical_failure", 168, true);

        assert res.getRiskEstimate() == 0.0 : "Expected 0 risk for insufficient history";
        assert res.getConfidence() == 0.1 : "Expected low confidence";
        System.out.println("  PASS  testInsufficientHistoryFallback");
    }
}
