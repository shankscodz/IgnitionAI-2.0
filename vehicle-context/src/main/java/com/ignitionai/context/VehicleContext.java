package com.ignitionai.context;

public class VehicleContext {
    private final String contextVersion;
    private final Long contextTimestampMs;
    private final Identity identity;
    private final Configuration configuration;
    private final OperatingConditions operatingConditions;
    private final EvidenceQuality evidenceQuality;

    public VehicleContext(String contextVersion, Long contextTimestampMs, Identity identity, 
                          Configuration configuration, OperatingConditions operatingConditions, 
                          EvidenceQuality evidenceQuality) {
        this.contextVersion = contextVersion;
        this.contextTimestampMs = contextTimestampMs;
        this.identity = identity;
        this.configuration = configuration;
        this.operatingConditions = operatingConditions;
        this.evidenceQuality = evidenceQuality;
    }

    public String getContextVersion() { return contextVersion; }
    public Long getContextTimestampMs() { return contextTimestampMs; }
    public Identity getIdentity() { return identity; }
    public Configuration getConfiguration() { return configuration; }
    public OperatingConditions getOperatingConditions() { return operatingConditions; }
    public EvidenceQuality getEvidenceQuality() { return evidenceQuality; }
}
