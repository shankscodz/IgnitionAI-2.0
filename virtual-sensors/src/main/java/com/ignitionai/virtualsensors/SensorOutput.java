package com.ignitionai.virtualsensors;

public class SensorOutput {
    public enum Status {
        AVAILABLE,
        MISSING_INPUTS,
        UNSUPPORTED,
        NOT_APPLICABLE,
        EVALUATION_ERROR
    }

    private final String sensorId;
    private final Double value;
    private final Status status;
    private final String reason;

    public SensorOutput(String sensorId, Double value, Status status, String reason) {
        this.sensorId = sensorId;
        this.value = value;
        this.status = status;
        this.reason = reason;
    }

    public static SensorOutput missingInputs(String sensorId, String reason) {
        return new SensorOutput(sensorId, null, Status.MISSING_INPUTS, reason);
    }

    public static SensorOutput unsupported(String sensorId, String reason) {
        return new SensorOutput(sensorId, null, Status.UNSUPPORTED, reason);
    }

    public static SensorOutput notApplicable(String sensorId, String reason) {
        return new SensorOutput(sensorId, null, Status.NOT_APPLICABLE, reason);
    }

    public static SensorOutput error(String sensorId, String reason) {
        return new SensorOutput(sensorId, null, Status.EVALUATION_ERROR, reason);
    }

    public static SensorOutput available(String sensorId, Double value) {
        return new SensorOutput(sensorId, value, Status.AVAILABLE, "success");
    }

    public String getSensorId() { return sensorId; }
    public Double getValue() { return value; }
    public Status getStatus() { return status; }
    public String getReason() { return reason; }
}
