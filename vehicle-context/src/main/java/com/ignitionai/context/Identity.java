package com.ignitionai.context;

import java.util.List;
import java.util.Collections;

public class Identity {
    private final String vehicleId;
    private final String vin;
    private final String ecuIdentity;
    private final List<String> calibrationIdentifiers;
    private final IdentityStatus identityStatus;
    private final String identitySource;
    private final double identityConfidence;

    public enum IdentityStatus {
        VERIFIED, PARTIAL, UNKNOWN
    }

    public Identity(String vehicleId, String vin, String ecuIdentity, List<String> calibrationIdentifiers, 
                    IdentityStatus identityStatus, String identitySource, double identityConfidence) {
        this.vehicleId = vehicleId;
        this.vin = vin;
        this.ecuIdentity = ecuIdentity;
        this.calibrationIdentifiers = calibrationIdentifiers != null ? Collections.unmodifiableList(calibrationIdentifiers) : Collections.emptyList();
        this.identityStatus = identityStatus != null ? identityStatus : IdentityStatus.UNKNOWN;
        this.identitySource = identitySource;
        this.identityConfidence = identityConfidence;
    }

    public String getVehicleId() { return vehicleId; }
    public String getVin() { return vin; }
    public String getEcuIdentity() { return ecuIdentity; }
    public List<String> getCalibrationIdentifiers() { return calibrationIdentifiers; }
    public IdentityStatus getIdentityStatus() { return identityStatus; }
    public String getIdentitySource() { return identitySource; }
    public double getIdentityConfidence() { return identityConfidence; }
}
