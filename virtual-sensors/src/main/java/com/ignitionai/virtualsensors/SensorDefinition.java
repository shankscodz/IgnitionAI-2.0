package com.ignitionai.virtualsensors;

import java.util.List;
import java.util.Map;

public class SensorDefinition {
    private String sensorId;
    private String sensorVersion;
    private String displayName;
    private String observationKind = "MODEL_DERIVED";
    private List<String> inputSignalIds;
    private List<String> inputFeatureIds;
    private List<String> outputSignalIds;
    private String units;
    private Map<String, String> vehicleApplicability;
    private Map<String, String> operatingPreconditions;
    private String formulaOrModelReference;
    private String samplingRequirements;
    private String uncertaintyDefinition;
    private String implementationType;
    private String status;

    // Getters and Setters
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getSensorVersion() { return sensorVersion; }
    public void setSensorVersion(String sensorVersion) { this.sensorVersion = sensorVersion; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getObservationKind() { return observationKind; }
    public void setObservationKind(String observationKind) { this.observationKind = observationKind; }

    public List<String> getInputSignalIds() { return inputSignalIds; }
    public void setInputSignalIds(List<String> inputSignalIds) { this.inputSignalIds = inputSignalIds; }

    public List<String> getInputFeatureIds() { return inputFeatureIds; }
    public void setInputFeatureIds(List<String> inputFeatureIds) { this.inputFeatureIds = inputFeatureIds; }

    public List<String> getOutputSignalIds() { return outputSignalIds; }
    public void setOutputSignalIds(List<String> outputSignalIds) { this.outputSignalIds = outputSignalIds; }

    public String getUnits() { return units; }
    public void setUnits(String units) { this.units = units; }

    public Map<String, String> getVehicleApplicability() { return vehicleApplicability; }
    public void setVehicleApplicability(Map<String, String> vehicleApplicability) { this.vehicleApplicability = vehicleApplicability; }

    public Map<String, String> getOperatingPreconditions() { return operatingPreconditions; }
    public void setOperatingPreconditions(Map<String, String> operatingPreconditions) { this.operatingPreconditions = operatingPreconditions; }

    public String getFormulaOrModelReference() { return formulaOrModelReference; }
    public void setFormulaOrModelReference(String formulaOrModelReference) { this.formulaOrModelReference = formulaOrModelReference; }

    public String getSamplingRequirements() { return samplingRequirements; }
    public void setSamplingRequirements(String samplingRequirements) { this.samplingRequirements = samplingRequirements; }

    public String getUncertaintyDefinition() { return uncertaintyDefinition; }
    public void setUncertaintyDefinition(String uncertaintyDefinition) { this.uncertaintyDefinition = uncertaintyDefinition; }

    public String getImplementationType() { return implementationType; }
    public void setImplementationType(String implementationType) { this.implementationType = implementationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
