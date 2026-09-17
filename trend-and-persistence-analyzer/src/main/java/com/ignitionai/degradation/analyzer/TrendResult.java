package com.ignitionai.degradation.analyzer;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import java.util.List;

public class TrendResult {
    private Double trendSlope;
    private TrendDirection direction;
    private Double latentState;
    private Double uncertainty;

    public TrendResult(Double trendSlope, TrendDirection direction, Double latentState, Double uncertainty) {
        this.trendSlope = trendSlope;
        this.direction = direction;
        this.latentState = latentState;
        this.uncertainty = uncertainty;
    }

    public Double getTrendSlope() { return trendSlope; }
    public TrendDirection getDirection() { return direction; }
    public Double getLatentState() { return latentState; }
    public Double getUncertainty() { return uncertainty; }
}
