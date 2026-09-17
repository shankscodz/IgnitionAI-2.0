package com.ignitionai.context;

public class VehicleContext {
    private Identity identity;
    private Configuration configuration;
    private OperatingConditions operatingConditions;
    private EvidenceQuality evidenceQuality;

    public VehicleContext(String vehicleId) {
        this.identity = new Identity(vehicleId);
        this.configuration = new Configuration();
        this.operatingConditions = new OperatingConditions();
        this.evidenceQuality = new EvidenceQuality();
    }

    public Identity getIdentity() { return identity; }
    public Configuration getConfiguration() { return configuration; }
    public OperatingConditions getOperatingConditions() { return operatingConditions; }
    public EvidenceQuality getEvidenceQuality() { return evidenceQuality; }
}
