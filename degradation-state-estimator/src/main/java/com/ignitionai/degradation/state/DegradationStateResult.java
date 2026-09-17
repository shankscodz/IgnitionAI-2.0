package com.ignitionai.degradation.state;

import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

public class DegradationStateResult {
    private DegradationState state;
    private Double score;

    public DegradationStateResult(DegradationState state, Double score) {
        this.state = state;
        this.score = score;
    }

    public DegradationState getState() { return state; }
    public Double getScore() { return score; }
}
