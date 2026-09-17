package com.ignitionai.degradation.risk;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.degradation.state.DegradationStateResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;
import com.ignitionai.phase5.DegradationOutput.RiskStatus;

public class HazardModelTest {
    public static void main(String[] args) {
        System.out.println("=== HazardModel Tests ===");

        testCriticalFailureRisk();
        testUnsupportedEndpoint();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testCriticalFailureRisk() {
        DegradationFeatures f = new DegradationFeatures();
        f.setRecurrenceCount(10);
        
        // High latent state
        TrendResult t = new TrendResult(0.1, TrendDirection.WORSENING, 0.9, 0.05);
        DegradationStateResult s = new DegradationStateResult(DegradationState.WORSENING_DEGRADATION, 0.9);

        EventRiskEstimator est = new EventRiskEstimator();
        EventRiskResult res = est.estimateRisk(f, t, s, "critical_failure", 168, true);

        assert res.getStatus() == RiskStatus.AVAILABLE : "Expected risk status AVAILABLE";
        assert res.getRiskEstimate() > 0.5 : "Expected high risk for critical_failure with high latent state";
        
        System.out.println("  PASS  testCriticalFailureRisk");
    }

    private static void testUnsupportedEndpoint() {
        EventRiskEstimator est = new EventRiskEstimator();
        EventRiskResult res = est.estimateRisk(new DegradationFeatures(), 
                                               new TrendResult(0.0, TrendDirection.UNKNOWN, 0.1, 0.1), 
                                               new DegradationStateResult(DegradationState.HEALTHY, 0.1), 
                                               "non_existent_failure", 168, true);

        assert res.getStatus() == RiskStatus.UNSUPPORTED_ENDPOINT : "Expected UNSUPPORTED_ENDPOINT";
        assert res.getRiskEstimate() == 0.0 : "Expected 0 risk for unsupported endpoint";
        
        System.out.println("  PASS  testUnsupportedEndpoint");
    }
}
