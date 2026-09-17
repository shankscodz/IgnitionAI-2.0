package com.ignitionai.context;

public class Configuration {
    private final String manufacturer;
    private final String model;
    private final Integer modelYear;
    private final String engineFamily;
    private final String engineCode;
    private final String fuelType;
    private final String transmission;
    private final String market;
    private final String softwareVariant;
    private final String applicabilityEvidence;
    private final DataState state;

    public Configuration(String manufacturer, String model, Integer modelYear, String engineFamily, 
                         String engineCode, String fuelType, String transmission, String market, 
                         String softwareVariant, String applicabilityEvidence, DataState state) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.modelYear = modelYear;
        this.engineFamily = engineFamily;
        this.engineCode = engineCode;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.market = market;
        this.softwareVariant = softwareVariant;
        this.applicabilityEvidence = applicabilityEvidence;
        this.state = state != null ? state : DataState.UNKNOWN;
    }

    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public Integer getModelYear() { return modelYear; }
    public String getEngineFamily() { return engineFamily; }
    public String getEngineCode() { return engineCode; }
    public String getFuelType() { return fuelType; }
    public String getTransmission() { return transmission; }
    public String getMarket() { return market; }
    public String getSoftwareVariant() { return softwareVariant; }
    public String getApplicabilityEvidence() { return applicabilityEvidence; }
    public DataState getState() { return state; }
}
