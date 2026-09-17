package com.ignitionai.degradation.features;

import com.ignitionai.phase4.AnomalyEpisode;

import java.util.List;

public class DegradationFeatures {
    private Double anomalyFrequency; // episodes per hour
    private Double averageDurationMs;
    private Integer recurrenceCount;
    private Double maxSeverity;
    private Double meanPersistence;
    private Long timeSinceLastAnomalyMs;
    private Double normalizedScore;

    public Double getAnomalyFrequency() { return anomalyFrequency; }
    public void setAnomalyFrequency(Double anomalyFrequency) { this.anomalyFrequency = anomalyFrequency; }
    public Double getAverageDurationMs() { return averageDurationMs; }
    public void setAverageDurationMs(Double averageDurationMs) { this.averageDurationMs = averageDurationMs; }
    public Integer getRecurrenceCount() { return recurrenceCount; }
    public void setRecurrenceCount(Integer recurrenceCount) { this.recurrenceCount = recurrenceCount; }
    public Double getMaxSeverity() { return maxSeverity; }
    public void setMaxSeverity(Double maxSeverity) { this.maxSeverity = maxSeverity; }
    public Double getMeanPersistence() { return meanPersistence; }
    public void setMeanPersistence(Double meanPersistence) { this.meanPersistence = meanPersistence; }
    public Long getTimeSinceLastAnomalyMs() { return timeSinceLastAnomalyMs; }
    public void setTimeSinceLastAnomalyMs(Long timeSinceLastAnomalyMs) { this.timeSinceLastAnomalyMs = timeSinceLastAnomalyMs; }
    public Double getNormalizedScore() { return normalizedScore; }
    public void setNormalizedScore(Double normalizedScore) { this.normalizedScore = normalizedScore; }
}
