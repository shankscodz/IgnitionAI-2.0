package com.ignitionai.healthevidence;

import com.ignitionai.phase6.VehicleHealthAssessment;
import java.util.UUID;

public class HealthEvidenceRecord {
    private final String evidenceId;
    private final String vehicleId;
    private final Long timestampMs;
    private final VehicleHealthAssessment assessment;

    public HealthEvidenceRecord(String vehicleId, Long timestampMs, VehicleHealthAssessment assessment) {
        this.evidenceId = UUID.randomUUID().toString();
        this.vehicleId = vehicleId;
        this.timestampMs = timestampMs;
        this.assessment = assessment;
    }

    public String getEvidenceId() { return evidenceId; }
    public String getVehicleId() { return vehicleId; }
    public Long getTimestampMs() { return timestampMs; }
    public VehicleHealthAssessment getAssessment() { return assessment; }
}
