package com.ignitionai.degradation.risk;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.degradation.state.DegradationStateResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.RiskStatus;

public class EventRiskEstimator {

    public EventRiskResult estimateRisk(DegradationFeatures features, TrendResult trend, DegradationStateResult state, 
                                        String endpointLabel, long predictionHorizonHours, boolean isCalibrated) {
        
        if (state.getState() == DegradationState.INSUFFICIENT_HISTORY || state.getState() == DegradationState.INSUFFICIENT_DATA || trend.getUncertainty() > 0.8) {
            EventRiskResult res = new EventRiskResult(0.0, predictionHorizonHours * 3600000L, 0.1, trend.getUncertainty());
            res.setStatus(RiskStatus.INSUFFICIENT_HISTORY);
            return res;
        }

        HazardModelConfig config = HazardModelConfig.get(endpointLabel);
        if (config == null) {
            EventRiskResult res = new EventRiskResult(0.0, predictionHorizonHours * 3600000L, 0.0, 1.0);
            res.setStatus(RiskStatus.UNSUPPORTED_ENDPOINT);
            return res;
        }
        
        if (!isCalibrated) {
            EventRiskResult res = new EventRiskResult(0.0, predictionHorizonHours * 3600000L, 0.0, 1.0);
            res.setStatus(RiskStatus.UNCALIBRATED);
            return res;
        }

        double latentState = trend.getLatentState();
        double velocity = trend.getTrendSlope();
        double uncertainty = trend.getUncertainty();

        // Step-wise Survival Integration
        double survivalProb = 1.0;
        double stepHours = 24.0;
        int steps = (int) (predictionHorizonHours / stepHours);
        if (steps == 0) steps = 1;

        for (int i = 0; i < steps; i++) {
            // Project the state forward dynamically
            double projectedState = latentState + velocity * (i * stepHours);
            
            // Calculate conditional hazard for this step
            double stepHazard = config.getBaselineHazard() * Math.exp(config.getBetaCoefficient() * projectedState);
            stepHazard = Math.min(0.999, stepHazard);
            
            survivalProb *= (1.0 - stepHazard);
        }
        
        // Event Risk = 1 - S(t)
        double riskEstimate = 1.0 - survivalProb;

        // Confidence inversely relates to the State-Space uncertainty
        double confidence = Math.max(0.1, 1.0 - Math.min(1.0, uncertainty * 0.5));
        
        EventRiskResult res = new EventRiskResult(riskEstimate, predictionHorizonHours * 3600000L, confidence, uncertainty);
        res.setStatus(RiskStatus.AVAILABLE);
        return res;
    }
}
