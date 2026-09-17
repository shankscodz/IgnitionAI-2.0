package com.ignitionai.phase4;

import java.util.List;
import java.util.Map;

public class AnomalyEpisode {
    private String vehicleId;
    private String sessionId;
    private String subsystemId;
    private String episodeId;
    private Long startTimeMs;
    private Long endTimeMs;
    private Double severity; // e.g. 0.0 to 1.0
    private Double durationMs;
    private Integer recurrenceCount;
    private Double persistence;
    private String operatingRegime;
    private Map<String, Double> qualityIndicators;
    private List<String> evidenceReferences;
    private String contextVersion;
    private String modelVersion;
    private String calibrationVersion;
    private String maintenanceAction;

    public AnomalyEpisode() {}

    public AnomalyEpisode(String vehicleId, String sessionId, String subsystemId, String episodeId, 
                          Long startTimeMs, Long endTimeMs, Double severity, Double durationMs, 
                          Integer recurrenceCount, Double persistence, String operatingRegime, 
                          Map<String, Double> qualityIndicators, List<String> evidenceReferences, 
                          String contextVersion, String modelVersion, String calibrationVersion, 
                          String maintenanceAction) {
        this.vehicleId = vehicleId;
        this.sessionId = sessionId;
        this.subsystemId = subsystemId;
        this.episodeId = episodeId;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.severity = severity;
        this.durationMs = durationMs;
        this.recurrenceCount = recurrenceCount;
        this.persistence = persistence;
        this.operatingRegime = operatingRegime;
        this.qualityIndicators = qualityIndicators;
        this.evidenceReferences = evidenceReferences;
        this.contextVersion = contextVersion;
        this.modelVersion = modelVersion;
        this.calibrationVersion = calibrationVersion;
        this.maintenanceAction = maintenanceAction;
    }

    public String getVehicleId() { return vehicleId; }
    public String getSessionId() { return sessionId; }
    public String getSubsystemId() { return subsystemId; }
    public String getEpisodeId() { return episodeId; }
    public Long getStartTimeMs() { return startTimeMs; }
    public Long getEndTimeMs() { return endTimeMs; }
    public Double getSeverity() { return severity; }
    public Double getDurationMs() { return durationMs; }
    public Integer getRecurrenceCount() { return recurrenceCount; }
    public Double getPersistence() { return persistence; }
    public String getOperatingRegime() { return operatingRegime; }
    public Map<String, Double> getQualityIndicators() { return qualityIndicators; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public String getContextVersion() { return contextVersion; }
    public String getModelVersion() { return modelVersion; }
    public String getCalibrationVersion() { return calibrationVersion; }
    public String getMaintenanceAction() { return maintenanceAction; }

    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setSubsystemId(String subsystemId) { this.subsystemId = subsystemId; }
    public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    public void setStartTimeMs(Long startTimeMs) { this.startTimeMs = startTimeMs; }
    public void setEndTimeMs(Long endTimeMs) { this.endTimeMs = endTimeMs; }
    public void setSeverity(Double severity) { this.severity = severity; }
    public void setDurationMs(Double durationMs) { this.durationMs = durationMs; }
    public void setRecurrenceCount(Integer recurrenceCount) { this.recurrenceCount = recurrenceCount; }
    public void setPersistence(Double persistence) { this.persistence = persistence; }
    public void setOperatingRegime(String operatingRegime) { this.operatingRegime = operatingRegime; }
    public void setQualityIndicators(Map<String, Double> qualityIndicators) { this.qualityIndicators = qualityIndicators; }
    public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }
    public void setContextVersion(String contextVersion) { this.contextVersion = contextVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setCalibrationVersion(String calibrationVersion) { this.calibrationVersion = calibrationVersion; }
    public void setMaintenanceAction(String maintenanceAction) { this.maintenanceAction = maintenanceAction; }
}
