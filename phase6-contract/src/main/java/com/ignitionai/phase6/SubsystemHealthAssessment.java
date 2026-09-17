package com.ignitionai.phase6;

import java.util.List;

public class SubsystemHealthAssessment {
    private final String subsystemId;
    private final SeverityLevel severityLevel;
    private final Double healthScore;
    private final Double confidence;
    private final Double uncertainty;
    private final List<String> degradedDataFlags;
    
    public SubsystemHealthAssessment(String subsystemId, SeverityLevel severityLevel, 
                                     Double healthScore, Double confidence, Double uncertainty, 
                                     List<String> degradedDataFlags) {
        this.subsystemId = subsystemId;
        this.severityLevel = severityLevel;
        this.healthScore = healthScore;
        this.confidence = confidence;
        this.uncertainty = uncertainty;
        this.degradedDataFlags = degradedDataFlags;
    }

    public String getSubsystemId() { return subsystemId; }
    public SeverityLevel getSeverityLevel() { return severityLevel; }
    public Double getHealthScore() { return healthScore; }
    public Double getConfidence() { return confidence; }
    public Double getUncertainty() { return uncertainty; }
    public List<String> getDegradedDataFlags() { return degradedDataFlags; }
}
