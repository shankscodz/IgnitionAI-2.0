package com.ignitionai.context;

import java.util.Collections;
import java.util.List;

public class OperatingConditions {
    private final Boolean engineRunningState;
    private final Double rpm;
    private final Double vehicleSpeed;
    private final Double load;
    private final Double throttle;
    private final Double coolantTemperature;
    private final Double intakeTemperature;
    private final Double ambientTemperature;
    
    private final OperatingRegime operatingRegime;
    private final OperatingRegime regimeTransition;
    private final List<OperatingRegime> transitionHistory;
    private final Long elapsedSessionTimeMs;
    private final DataState state;

    public OperatingConditions(Boolean engineRunningState, Double rpm, Double vehicleSpeed, Double load, 
                               Double throttle, Double coolantTemperature, Double intakeTemperature, 
                               Double ambientTemperature, OperatingRegime operatingRegime, 
                               OperatingRegime regimeTransition, List<OperatingRegime> transitionHistory,
                               Long elapsedSessionTimeMs, DataState state) {
        this.engineRunningState = engineRunningState;
        this.rpm = rpm;
        this.vehicleSpeed = vehicleSpeed;
        this.load = load;
        this.throttle = throttle;
        this.coolantTemperature = coolantTemperature;
        this.intakeTemperature = intakeTemperature;
        this.ambientTemperature = ambientTemperature;
        this.operatingRegime = operatingRegime != null ? operatingRegime : OperatingRegime.UNKNOWN;
        this.regimeTransition = regimeTransition;
        this.transitionHistory = transitionHistory != null ? Collections.unmodifiableList(transitionHistory) : Collections.emptyList();
        this.elapsedSessionTimeMs = elapsedSessionTimeMs;
        this.state = state != null ? state : DataState.UNKNOWN;
    }

    public Boolean getEngineRunningState() { return engineRunningState; }
    public Double getRpm() { return rpm; }
    public Double getVehicleSpeed() { return vehicleSpeed; }
    public Double getLoad() { return load; }
    public Double getThrottle() { return throttle; }
    public Double getCoolantTemperature() { return coolantTemperature; }
    public Double getIntakeTemperature() { return intakeTemperature; }
    public Double getAmbientTemperature() { return ambientTemperature; }
    public OperatingRegime getOperatingRegime() { return operatingRegime; }
    public OperatingRegime getRegimeTransition() { return regimeTransition; }
    public List<OperatingRegime> getTransitionHistory() { return transitionHistory; }
    public Long getElapsedSessionTimeMs() { return elapsedSessionTimeMs; }
    public DataState getState() { return state; }
}
