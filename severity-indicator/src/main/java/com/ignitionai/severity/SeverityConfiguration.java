package com.ignitionai.severity;

public class SeverityConfiguration {
    private final double criticalDegradationScoreThreshold;
    private final double criticalEventRiskThreshold;
    
    private final double degradedDegradationScoreThreshold;
    private final double degradedEventRiskThreshold;
    
    private final double watchDegradationScoreThreshold;
    private final double watchEventRiskThreshold;

    public SeverityConfiguration(double criticalDegradationScoreThreshold, double criticalEventRiskThreshold,
                                 double degradedDegradationScoreThreshold, double degradedEventRiskThreshold,
                                 double watchDegradationScoreThreshold, double watchEventRiskThreshold) {
        this.criticalDegradationScoreThreshold = criticalDegradationScoreThreshold;
        this.criticalEventRiskThreshold = criticalEventRiskThreshold;
        this.degradedDegradationScoreThreshold = degradedDegradationScoreThreshold;
        this.degradedEventRiskThreshold = degradedEventRiskThreshold;
        this.watchDegradationScoreThreshold = watchDegradationScoreThreshold;
        this.watchEventRiskThreshold = watchEventRiskThreshold;
    }

    public static SeverityConfiguration getDefault() {
        return new SeverityConfiguration(
            80.0, 0.8,  // Critical
            50.0, 0.5,  // Degraded
            20.0, 0.2   // Watch
        );
    }

    public double getCriticalDegradationScoreThreshold() { return criticalDegradationScoreThreshold; }
    public double getCriticalEventRiskThreshold() { return criticalEventRiskThreshold; }
    public double getDegradedDegradationScoreThreshold() { return degradedDegradationScoreThreshold; }
    public double getDegradedEventRiskThreshold() { return degradedEventRiskThreshold; }
    public double getWatchDegradationScoreThreshold() { return watchDegradationScoreThreshold; }
    public double getWatchEventRiskThreshold() { return watchEventRiskThreshold; }
}
