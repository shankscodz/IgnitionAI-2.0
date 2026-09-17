package com.ignitionai.context;

public class OperatingConditions {
    private Boolean engineRunningState;
    private Double rpm;
    private Double vehicleSpeed;
    private Double load;
    private Double throttle;
    private Double coolantTemperature;
    private Double intakeTemperature;
    private Double ambientTemperature;
    
    private OperatingRegime operatingRegime = OperatingRegime.UNKNOWN;
    private OperatingRegime regimeTransition;
    private Long elapsedSessionTimeMs;

    private DataState state = DataState.UNKNOWN;

    // Getters and setters
    public Boolean getEngineRunningState() { return engineRunningState; }
    public void setEngineRunningState(Boolean engineRunningState) { this.engineRunningState = engineRunningState; }

    public Double getRpm() { return rpm; }
    public void setRpm(Double rpm) { this.rpm = rpm; }

    public Double getVehicleSpeed() { return vehicleSpeed; }
    public void setVehicleSpeed(Double vehicleSpeed) { this.vehicleSpeed = vehicleSpeed; }

    public Double getLoad() { return load; }
    public void setLoad(Double load) { this.load = load; }

    public Double getThrottle() { return throttle; }
    public void setThrottle(Double throttle) { this.throttle = throttle; }

    public Double getCoolantTemperature() { return coolantTemperature; }
    public void setCoolantTemperature(Double coolantTemperature) { this.coolantTemperature = coolantTemperature; }

    public Double getIntakeTemperature() { return intakeTemperature; }
    public void setIntakeTemperature(Double intakeTemperature) { this.intakeTemperature = intakeTemperature; }

    public Double getAmbientTemperature() { return ambientTemperature; }
    public void setAmbientTemperature(Double ambientTemperature) { this.ambientTemperature = ambientTemperature; }

    public OperatingRegime getOperatingRegime() { return operatingRegime; }
    public void setOperatingRegime(OperatingRegime operatingRegime) { this.operatingRegime = operatingRegime; }

    public OperatingRegime getRegimeTransition() { return regimeTransition; }
    public void setRegimeTransition(OperatingRegime regimeTransition) { this.regimeTransition = regimeTransition; }

    public Long getElapsedSessionTimeMs() { return elapsedSessionTimeMs; }
    public void setElapsedSessionTimeMs(Long elapsedSessionTimeMs) { this.elapsedSessionTimeMs = elapsedSessionTimeMs; }

    public DataState getState() { return state; }
    public void setState(DataState state) { this.state = state; }
}
