package com.ignitionai.phase5;

import java.util.List;

public class DegradationOutput {

    public enum DegradationState {
        HEALTHY,
        EARLY_DEGRADATION,
        STABLE_DEGRADATION,
        WORSENING_DEGRADATION,
        RECOVERED_DEGRADATION,
        INSUFFICIENT_HISTORY,
        INSUFFICIENT_DATA
    }

    public enum TrendDirection {
        IMPROVING,
        STABLE,
        WORSENING,
        UNKNOWN
    }

    public enum DataSufficiency {
        SUFFICIENT,
        INSUFFICIENT,
        SPARSE
    }

    public enum RiskStatus {
        AVAILABLE,
        INSUFFICIENT_HISTORY,
        UNSUPPORTED_ENDPOINT,
        UNCALIBRATED
    }

    private String vehicleId;
    private String subsystemId;
    private Long evaluationTimeMs;
    private DegradationState degradationState;
    private Double degradationScore;
    private TrendDirection trendDirection;
    private Double trendSlope;
    private Double persistenceScore;
    private Double recurrenceScore;
    private Long timeSincePreviousEventMs;
    
    // New fields
    private Double observedExposure;
    private Integer requiredHistory;
    private DataSufficiency dataSufficiencyStatus;
    
    // Risk Model fields
    private String endpointLabel;
    private Long timeOriginMs;
    private String exposureUnits;
    private Boolean isCensored;
    private RiskStatus riskStatus;

    private Double eventRiskEstimate; // Probability [0,1]
    private Long predictionHorizonMs;
    private Double confidence;
    private Double uncertainty;
    private List<String> evidenceReferences;
    private String modelVersion;
    private String configurationVersion;

    public DegradationOutput() {}

    public String getVehicleId() { return vehicleId; }
    public String getSubsystemId() { return subsystemId; }
    public Long getEvaluationTimeMs() { return evaluationTimeMs; }
    public DegradationState getDegradationState() { return degradationState; }
    public Double getDegradationScore() { return degradationScore; }
    public TrendDirection getTrendDirection() { return trendDirection; }
    public Double getTrendSlope() { return trendSlope; }
    public Double getPersistenceScore() { return persistenceScore; }
    public Double getRecurrenceScore() { return recurrenceScore; }
    public Long getTimeSincePreviousEventMs() { return timeSincePreviousEventMs; }
    public Double getObservedExposure() { return observedExposure; }
    public Integer getRequiredHistory() { return requiredHistory; }
    public DataSufficiency getDataSufficiencyStatus() { return dataSufficiencyStatus; }
    public String getEndpointLabel() { return endpointLabel; }
    public Long getTimeOriginMs() { return timeOriginMs; }
    public String getExposureUnits() { return exposureUnits; }
    public Boolean getIsCensored() { return isCensored; }
    public RiskStatus getRiskStatus() { return riskStatus; }
    public Double getEventRiskEstimate() { return eventRiskEstimate; }
    public Long getPredictionHorizonMs() { return predictionHorizonMs; }
    public Double getConfidence() { return confidence; }
    public Double getUncertainty() { return uncertainty; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public String getModelVersion() { return modelVersion; }
    public String getConfigurationVersion() { return configurationVersion; }

    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public void setSubsystemId(String subsystemId) { this.subsystemId = subsystemId; }
    public void setEvaluationTimeMs(Long evaluationTimeMs) { this.evaluationTimeMs = evaluationTimeMs; }
    public void setDegradationState(DegradationState degradationState) { this.degradationState = degradationState; }
    public void setDegradationScore(Double degradationScore) { this.degradationScore = degradationScore; }
    public void setTrendDirection(TrendDirection trendDirection) { this.trendDirection = trendDirection; }
    public void setTrendSlope(Double trendSlope) { this.trendSlope = trendSlope; }
    public void setPersistenceScore(Double persistenceScore) { this.persistenceScore = persistenceScore; }
    public void setRecurrenceScore(Double recurrenceScore) { this.recurrenceScore = recurrenceScore; }
    public void setTimeSincePreviousEventMs(Long timeSincePreviousEventMs) { this.timeSincePreviousEventMs = timeSincePreviousEventMs; }
    public void setObservedExposure(Double observedExposure) { this.observedExposure = observedExposure; }
    public void setRequiredHistory(Integer requiredHistory) { this.requiredHistory = requiredHistory; }
    public void setDataSufficiencyStatus(DataSufficiency dataSufficiencyStatus) { this.dataSufficiencyStatus = dataSufficiencyStatus; }
    public void setEndpointLabel(String endpointLabel) { this.endpointLabel = endpointLabel; }
    public void setTimeOriginMs(Long timeOriginMs) { this.timeOriginMs = timeOriginMs; }
    public void setExposureUnits(String exposureUnits) { this.exposureUnits = exposureUnits; }
    public void setIsCensored(Boolean isCensored) { this.isCensored = isCensored; }
    public void setRiskStatus(RiskStatus riskStatus) { this.riskStatus = riskStatus; }
    public void setEventRiskEstimate(Double eventRiskEstimate) { this.eventRiskEstimate = eventRiskEstimate; }
    public void setPredictionHorizonMs(Long predictionHorizonMs) { this.predictionHorizonMs = predictionHorizonMs; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public void setUncertainty(Double uncertainty) { this.uncertainty = uncertainty; }
    public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setConfigurationVersion(String configurationVersion) { this.configurationVersion = configurationVersion; }
}
