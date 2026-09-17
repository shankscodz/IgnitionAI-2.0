package com.ignitionai.phase6;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class VehicleHealthAssessment {
    private final String vehicleId;
    private final Long assessmentTimestampMs;
    private final Long assessmentWindowMs;
    
    private final Double vehicleHealthIndex;
    private final VehicleHealthBand vehicleHealthBand;
    
    private final List<SubsystemHealthAssessment> subsystemAssessments;
    private final Map<String, Double> componentWeights;
    
    private final List<String> evidenceReferences;
    private final String calculationVersion;
    private final String registryReleaseVersion;
    private final String contextVersion;
    
    public VehicleHealthAssessment(String vehicleId, Long assessmentTimestampMs, Long assessmentWindowMs,
                                   Double vehicleHealthIndex, VehicleHealthBand vehicleHealthBand,
                                   List<SubsystemHealthAssessment> subsystemAssessments,
                                   Map<String, Double> componentWeights,
                                   List<String> evidenceReferences, String calculationVersion,
                                   String registryReleaseVersion, String contextVersion) {
        this.vehicleId = vehicleId;
        this.assessmentTimestampMs = assessmentTimestampMs;
        this.assessmentWindowMs = assessmentWindowMs;
        this.vehicleHealthIndex = vehicleHealthIndex;
        this.vehicleHealthBand = vehicleHealthBand;
        this.subsystemAssessments = subsystemAssessments != null ? Collections.unmodifiableList(subsystemAssessments) : Collections.emptyList();
        this.componentWeights = componentWeights != null ? Collections.unmodifiableMap(componentWeights) : Collections.emptyMap();
        this.evidenceReferences = evidenceReferences != null ? Collections.unmodifiableList(evidenceReferences) : Collections.emptyList();
        this.calculationVersion = calculationVersion;
        this.registryReleaseVersion = registryReleaseVersion;
        this.contextVersion = contextVersion;
    }

    public String getVehicleId() { return vehicleId; }
    public Long getAssessmentTimestampMs() { return assessmentTimestampMs; }
    public Long getAssessmentWindowMs() { return assessmentWindowMs; }
    public Double getVehicleHealthIndex() { return vehicleHealthIndex; }
    public VehicleHealthBand getVehicleHealthBand() { return vehicleHealthBand; }
    public List<SubsystemHealthAssessment> getSubsystemAssessments() { return subsystemAssessments; }
    public Map<String, Double> getComponentWeights() { return componentWeights; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public String getCalculationVersion() { return calculationVersion; }
    public String getRegistryReleaseVersion() { return registryReleaseVersion; }
    public String getContextVersion() { return contextVersion; }
}
