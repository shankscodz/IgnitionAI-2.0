package com.ignitionai.degradation.analyzer;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import java.util.List;

public class TrendAnalyzer {
    
    private double processNoiseVarX = 0.01;
    private double processNoiseVarV = 0.001;
    private double defaultMeasurementNoiseVar = 0.1;

    public TrendAnalyzer() {}

    public TrendAnalyzer(double processNoiseVarX, double processNoiseVarV, double defaultMeasurementNoiseVar) {
        this.processNoiseVarX = processNoiseVarX;
        this.processNoiseVarV = processNoiseVarV;
        this.defaultMeasurementNoiseVar = defaultMeasurementNoiseVar;
    }

    public TrendResult analyzeTrend(AnomalyEpisode currentEpisode, List<AnomalyEpisode> history) {
        if (history == null || history.isEmpty()) {
            return new TrendResult(0.0, TrendDirection.UNKNOWN, currentEpisode.getSeverity(), 1.0);
        }

        // Initialize state-space model using the first episode
        AnomalyEpisode first = history.get(0);
        StateSpaceModel kf = new StateSpaceModel(first.getSeverity(), processNoiseVarX, processNoiseVarV, defaultMeasurementNoiseVar);
        
        String currentCalibration = first.getCalibrationVersion();

        // Recursively update model
        long previousTime = first.getStartTimeMs();
        for (int i = 1; i < history.size(); i++) {
            AnomalyEpisode ep = history.get(i);
            
            // Check for discontinuities (calibration change or maintenance)
            if (ep.getMaintenanceAction() != null || 
               (ep.getCalibrationVersion() != null && !ep.getCalibrationVersion().equals(currentCalibration))) {
                kf.resetUncertainty();
                currentCalibration = ep.getCalibrationVersion();
            }
            
            double dtHours = (ep.getStartTimeMs() - previousTime) / 3600000.0;
            double confidence = 1.0;
            if (ep.getQualityIndicators() != null && ep.getQualityIndicators().containsKey("confidence")) {
                confidence = ep.getQualityIndicators().get("confidence");
            }
            
            kf.update(dtHours, ep.getSeverity(), confidence);
            previousTime = ep.getStartTimeMs();
        }

        // Add current episode
        if (currentEpisode.getMaintenanceAction() != null || 
           (currentEpisode.getCalibrationVersion() != null && !currentEpisode.getCalibrationVersion().equals(currentCalibration))) {
            kf.resetUncertainty();
        }

        double dtHoursCurrent = (currentEpisode.getStartTimeMs() - previousTime) / 3600000.0;
        double currentConfidence = 1.0;
        if (currentEpisode.getQualityIndicators() != null && currentEpisode.getQualityIndicators().containsKey("confidence")) {
            currentConfidence = currentEpisode.getQualityIndicators().get("confidence");
        }
        
        kf.update(dtHoursCurrent, currentEpisode.getSeverity(), currentConfidence);

        // Derive Trend Result
        double slope = kf.getVelocity();
        double state = kf.getState();
        double uncertainty = kf.getUncertainty();

        TrendDirection direction;
        // Worsening requires higher confidence to trigger if uncertainty is high
        double threshold = 0.05 + Math.min(uncertainty, 0.5); 
        
        if (slope > threshold) direction = TrendDirection.WORSENING;
        else if (slope < -threshold) direction = TrendDirection.IMPROVING;
        else direction = TrendDirection.STABLE;

        return new TrendResult(slope, direction, state, uncertainty);
    }
}
