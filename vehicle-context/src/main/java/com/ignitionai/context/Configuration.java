package com.ignitionai.context;

public class Configuration {
    private String manufacturer;
    private String model;
    private Integer modelYear;
    private String engineFamily;
    private String engineCode;
    private String fuelType;
    private String transmission;
    private String market;
    private String softwareVariant;
    private String applicabilityEvidence;

    private DataState state = DataState.UNKNOWN;

    // Getters and setters
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getEngineFamily() { return engineFamily; }
    public void setEngineFamily(String engineFamily) { this.engineFamily = engineFamily; }

    public String getEngineCode() { return engineCode; }
    public void setEngineCode(String engineCode) { this.engineCode = engineCode; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getSoftwareVariant() { return softwareVariant; }
    public void setSoftwareVariant(String softwareVariant) { this.softwareVariant = softwareVariant; }

    public String getApplicabilityEvidence() { return applicabilityEvidence; }
    public void setApplicabilityEvidence(String applicabilityEvidence) { this.applicabilityEvidence = applicabilityEvidence; }

    public DataState getState() { return state; }
    public void setState(DataState state) { this.state = state; }
}
