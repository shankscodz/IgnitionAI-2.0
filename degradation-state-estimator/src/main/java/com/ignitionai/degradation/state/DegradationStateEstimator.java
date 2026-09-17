package com.ignitionai.degradation.state;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

public class DegradationStateEstimator {

    public DegradationStateResult estimateState(DegradationFeatures features, TrendResult trend) {
        if (features.getNormalizedScore() == null) {
            return new DegradationStateResult(DegradationState.INSUFFICIENT_DATA, 0.0);
        }

        if (features.getRecurrenceCount() < 2) {
            // First few anomalies are generally EARLY or HEALTHY if they are very low severity
            if (features.getNormalizedScore() < 0.2) {
                return new DegradationStateResult(DegradationState.HEALTHY, features.getNormalizedScore());
            } else {
                return new DegradationStateResult(DegradationState.INSUFFICIENT_HISTORY, features.getNormalizedScore());
            }
        }

        double stateEstimate = trend.getLatentState();
        double uncertainty = trend.getUncertainty();

        DegradationState state;
        
        // Define statistical state boundaries
        // If state + 2*sigma is very low, it's HEALTHY or RECOVERED
        double upperBound = stateEstimate + 2 * Math.sqrt(uncertainty);
        double lowerBound = stateEstimate - 2 * Math.sqrt(uncertainty);

        if (upperBound < 0.2 && trend.getDirection() != TrendDirection.WORSENING) {
            if (trend.getDirection() == TrendDirection.IMPROVING) {
                state = DegradationState.RECOVERED_DEGRADATION;
            } else {
                state = DegradationState.HEALTHY;
            }
        } else if (trend.getDirection() == TrendDirection.WORSENING) {
            state = DegradationState.WORSENING_DEGRADATION;
        } else if (trend.getDirection() == TrendDirection.STABLE) {
            if (lowerBound > 0.4) {
                state = DegradationState.STABLE_DEGRADATION;
            } else {
                state = DegradationState.EARLY_DEGRADATION;
            }
        } else {
            state = DegradationState.EARLY_DEGRADATION; 
        }

        return new DegradationStateResult(state, stateEstimate);
    }
}
