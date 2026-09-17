package com.ignitionai.context;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvidenceQuality {
    private final Set<String> supportedSignals;
    private final Set<String> observedSignals;
    private final Set<String> missingSignals;
    private final Map<String, Double> samplingRates;
    private final List<String> gaps;
    private final Integer staleReadings;
    private final Integer invalidReadings;
    
    private final Boolean dtcAvailability;
    private final String readinessContext;
    private final List<String> previousSessionReferences;
    private final Long exposureDurationMs;
    private final String contextVersion;

    public EvidenceQuality(Set<String> supportedSignals, Set<String> observedSignals, Set<String> missingSignals, 
                           Map<String, Double> samplingRates, List<String> gaps, Integer staleReadings, 
                           Integer invalidReadings, Boolean dtcAvailability, String readinessContext, 
                           List<String> previousSessionReferences, Long exposureDurationMs, String contextVersion) {
        this.supportedSignals = supportedSignals != null ? Collections.unmodifiableSet(supportedSignals) : Collections.emptySet();
        this.observedSignals = observedSignals != null ? Collections.unmodifiableSet(observedSignals) : Collections.emptySet();
        this.missingSignals = missingSignals != null ? Collections.unmodifiableSet(missingSignals) : Collections.emptySet();
        this.samplingRates = samplingRates != null ? Collections.unmodifiableMap(samplingRates) : Collections.emptyMap();
        this.gaps = gaps != null ? Collections.unmodifiableList(gaps) : Collections.emptyList();
        this.staleReadings = staleReadings != null ? staleReadings : 0;
        this.invalidReadings = invalidReadings != null ? invalidReadings : 0;
        this.dtcAvailability = dtcAvailability;
        this.readinessContext = readinessContext;
        this.previousSessionReferences = previousSessionReferences != null ? Collections.unmodifiableList(previousSessionReferences) : Collections.emptyList();
        this.exposureDurationMs = exposureDurationMs;
        this.contextVersion = contextVersion;
    }

    public Set<String> getSupportedSignals() { return supportedSignals; }
    public Set<String> getObservedSignals() { return observedSignals; }
    public Set<String> getMissingSignals() { return missingSignals; }
    public Map<String, Double> getSamplingRates() { return samplingRates; }
    public List<String> getGaps() { return gaps; }
    public Integer getStaleReadings() { return staleReadings; }
    public Integer getInvalidReadings() { return invalidReadings; }
    public Boolean getDtcAvailability() { return dtcAvailability; }
    public String getReadinessContext() { return readinessContext; }
    public List<String> getPreviousSessionReferences() { return previousSessionReferences; }
    public Long getExposureDurationMs() { return exposureDurationMs; }
    public String getContextVersion() { return contextVersion; }
}
