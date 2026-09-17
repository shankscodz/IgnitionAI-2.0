package com.ignitionai.context;

import java.util.List;

public class Identity {
    private String vehicleId;
    private String vin;
    private String ecuIdentity;
    private List<String> calibrationIdentifiers;
    private IdentityStatus identityStatus;
    private String identitySource;
    private double identityConfidence;

    public enum IdentityStatus {
        VERIFIED, PARTIAL, UNKNOWN
    }

    public Identity(String vehicleId) {
        this.vehicleId = vehicleId;
        this.identityStatus = IdentityStatus.UNKNOWN;
    }

    // Getters and setters
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getEcuIdentity() { return ecuIdentity; }
    public void setEcuIdentity(String ecuIdentity) { this.ecuIdentity = ecuIdentity; }

    public List<String> getCalibrationIdentifiers() { return calibrationIdentifiers; }
    public void setCalibrationIdentifiers(List<String> calibrationIdentifiers) { this.calibrationIdentifiers = calibrationIdentifiers; }

    public IdentityStatus getIdentityStatus() { return identityStatus; }
    public void setIdentityStatus(IdentityStatus identityStatus) { this.identityStatus = identityStatus; }

    public String getIdentitySource() { return identitySource; }
    public void setIdentitySource(String identitySource) { this.identitySource = identitySource; }

    public double getIdentityConfidence() { return identityConfidence; }
    public void setIdentityConfidence(double identityConfidence) { this.identityConfidence = identityConfidence; }
}
