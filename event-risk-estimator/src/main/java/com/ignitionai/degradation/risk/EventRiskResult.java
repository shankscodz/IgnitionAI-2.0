package com.ignitionai.degradation.risk;

import com.ignitionai.phase5.DegradationOutput.RiskStatus;

public class EventRiskResult {
    private Double riskEstimate; // 0.0 to 1.0
    private Long predictionHorizonMs;
    private Double confidence;
    private Double uncertainty;
    private RiskStatus status;

    public EventRiskResult(Double riskEstimate, Long predictionHorizonMs, Double confidence, Double uncertainty) {
        this.riskEstimate = riskEstimate;
        this.predictionHorizonMs = predictionHorizonMs;
        this.confidence = confidence;
        this.uncertainty = uncertainty;
    }

    public Double getRiskEstimate() { return riskEstimate; }
    public Long getPredictionHorizonMs() { return predictionHorizonMs; }
    public Double getConfidence() { return confidence; }
    public Double getUncertainty() { return uncertainty; }
    public RiskStatus getStatus() { return status; }
    public void setStatus(RiskStatus status) { this.status = status; }
}
