package com.ignitionai.severity;

import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase5.DegradationOutput.DataSufficiency;
import com.ignitionai.phase6.SeverityLevel;

public class SeverityClassifier {

    private final SeverityConfiguration config;

    public SeverityClassifier(SeverityConfiguration config) {
        this.config = config != null ? config : SeverityConfiguration.getDefault();
    }

    public SeverityLevel classify(DegradationOutput output) {
        if (output == null) {
            return SeverityLevel.UNKNOWN;
        }

        if (output.getDataSufficiencyStatus() == DataSufficiency.INSUFFICIENT) {
            return SeverityLevel.UNKNOWN;
        }

        double degScore = output.getDegradationScore() != null ? output.getDegradationScore() : 0.0;
        double eventRisk = output.getEventRiskEstimate() != null ? output.getEventRiskEstimate() : 0.0;

        // CRITICAL checks
        if (degScore >= config.getCriticalDegradationScoreThreshold() || 
            eventRisk >= config.getCriticalEventRiskThreshold()) {
            return SeverityLevel.CRITICAL;
        }

        // DEGRADED checks
        if (degScore >= config.getDegradedDegradationScoreThreshold() || 
            eventRisk >= config.getDegradedEventRiskThreshold() ||
            output.getDegradationState() == DegradationOutput.DegradationState.WORSENING_DEGRADATION ||
            output.getDegradationState() == DegradationOutput.DegradationState.STABLE_DEGRADATION) {
            return SeverityLevel.DEGRADED;
        }

        // WATCH checks
        if (degScore >= config.getWatchDegradationScoreThreshold() || 
            eventRisk >= config.getWatchEventRiskThreshold() ||
            output.getDegradationState() == DegradationOutput.DegradationState.EARLY_DEGRADATION) {
            return SeverityLevel.WATCH;
        }

        // NORMAL
        if (output.getDegradationState() == DegradationOutput.DegradationState.HEALTHY ||
            output.getDegradationState() == DegradationOutput.DegradationState.RECOVERED_DEGRADATION) {
            return SeverityLevel.NORMAL;
        }

        return SeverityLevel.UNKNOWN;
    }
}
