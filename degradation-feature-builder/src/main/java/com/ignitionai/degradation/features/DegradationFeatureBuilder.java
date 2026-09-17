package com.ignitionai.degradation.features;

import com.ignitionai.phase4.AnomalyEpisode;

import java.util.List;

public class DegradationFeatureBuilder {

    public DegradationFeatures buildFeatures(AnomalyEpisode currentEpisode, List<AnomalyEpisode> history) {
        DegradationFeatures features = new DegradationFeatures();

        if (history == null || history.isEmpty()) {
            features.setRecurrenceCount(0);
            features.setAnomalyFrequency(0.0);
            features.setAverageDurationMs(currentEpisode.getDurationMs());
            features.setMaxSeverity(currentEpisode.getSeverity());
            features.setMeanPersistence(currentEpisode.getPersistence());
            features.setTimeSinceLastAnomalyMs(null);
            features.setNormalizedScore(calculateNormalizedScore(currentEpisode));
            return features;
        }

        // Calculate time since last anomaly (history is expected to be chronological)
        AnomalyEpisode previous = history.get(history.size() - 1);
        features.setTimeSinceLastAnomalyMs(currentEpisode.getStartTimeMs() - previous.getEndTimeMs());

        // Calculate recurrence and frequency
        features.setRecurrenceCount(history.size());
        
        long firstTime = history.get(0).getStartTimeMs();
        long timespanMs = currentEpisode.getEndTimeMs() - firstTime;
        double timespanHours = timespanMs / (1000.0 * 60 * 60);
        
        if (timespanHours > 0) {
            features.setAnomalyFrequency((history.size() + 1) / timespanHours);
        } else {
            features.setAnomalyFrequency(0.0);
        }

        // Aggregate duration, severity, persistence
        double totalDuration = currentEpisode.getDurationMs();
        double maxSev = currentEpisode.getSeverity();
        double totalPersist = currentEpisode.getPersistence();
        
        for (AnomalyEpisode ep : history) {
            totalDuration += ep.getDurationMs();
            if (ep.getSeverity() > maxSev) maxSev = ep.getSeverity();
            totalPersist += ep.getPersistence();
        }
        
        features.setAverageDurationMs(totalDuration / (history.size() + 1));
        features.setMaxSeverity(maxSev);
        features.setMeanPersistence(totalPersist / (history.size() + 1));

        features.setNormalizedScore(calculateNormalizedScore(currentEpisode));

        return features;
    }

    private Double calculateNormalizedScore(AnomalyEpisode episode) {
        // Data-quality weighting: scale the severity by confidence if available
        double baseScore = episode.getSeverity();
        double qualityWeight = 1.0;
        if (episode.getQualityIndicators() != null && episode.getQualityIndicators().containsKey("confidence")) {
            qualityWeight = episode.getQualityIndicators().get("confidence");
        }

        // Operating-condition normalization:
        // Example logic: anomalies during STEADY_CRUISE are more heavily weighted than ENGINE_OFF
        double regimeMultiplier = 1.0;
        if ("STEADY_CRUISE".equals(episode.getOperatingRegime())) {
            regimeMultiplier = 1.2;
        } else if ("ENGINE_OFF".equals(episode.getOperatingRegime())) {
            regimeMultiplier = 0.5;
        }

        return baseScore * qualityWeight * regimeMultiplier;
    }
}
