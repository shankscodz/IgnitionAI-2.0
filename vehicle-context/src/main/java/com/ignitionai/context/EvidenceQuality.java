package com.ignitionai.context;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvidenceQuality {
    private Set<String> supportedSignals;
    private Set<String> observedSignals;
    private Set<String> missingSignals;
    private Map<String, Double> samplingRates;
    private List<String> gaps;
    private Integer staleReadings = 0;
    private Integer invalidReadings = 0;
    
    private Boolean dtcAvailability;
    private String readinessContext;
    private List<String> previousSessionReferences;
    private Long exposureDurationMs;
    private String contextVersion;

    // Getters and setters
    public Set<String> getSupportedSignals() { return supportedSignals; }
    public void setSupportedSignals(Set<String> supportedSignals) { this.supportedSignals = supportedSignals; }

    public Set<String> getObservedSignals() { return observedSignals; }
    public void setObservedSignals(Set<String> observedSignals) { this.observedSignals = observedSignals; }

    public Set<String> getMissingSignals() { return missingSignals; }
    public void setMissingSignals(Set<String> missingSignals) { this.missingSignals = missingSignals; }

    public Map<String, Double> getSamplingRates() { return samplingRates; }
    public void setSamplingRates(Map<String, Double> samplingRates) { this.samplingRates = samplingRates; }

    public List<String> getGaps() { return gaps; }
    public void setGaps(List<String> gaps) { this.gaps = gaps; }

    public Integer getStaleReadings() { return staleReadings; }
    public void setStaleReadings(Integer staleReadings) { this.staleReadings = staleReadings; }

    public Integer getInvalidReadings() { return invalidReadings; }
    public void setInvalidReadings(Integer invalidReadings) { this.invalidReadings = invalidReadings; }

    public Boolean getDtcAvailability() { return dtcAvailability; }
    public void setDtcAvailability(Boolean dtcAvailability) { this.dtcAvailability = dtcAvailability; }

    public String getReadinessContext() { return readinessContext; }
    public void setReadinessContext(String readinessContext) { this.readinessContext = readinessContext; }

    public List<String> getPreviousSessionReferences() { return previousSessionReferences; }
    public void setPreviousSessionReferences(List<String> previousSessionReferences) { this.previousSessionReferences = previousSessionReferences; }

    public Long getExposureDurationMs() { return exposureDurationMs; }
    public void setExposureDurationMs(Long exposureDurationMs) { this.exposureDurationMs = exposureDurationMs; }

    public String getContextVersion() { return contextVersion; }
    public void setContextVersion(String contextVersion) { this.contextVersion = contextVersion; }
}
