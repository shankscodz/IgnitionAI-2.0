package com.ignitionai.healthscore;

import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase5.DegradationOutput.DataSufficiency;
import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.SeverityLevel;
import com.ignitionai.severity.SeverityClassifier;

import java.util.ArrayList;
import java.util.List;

public class SubsystemScoreCalculator {
    
    private final SeverityClassifier severityClassifier;

    public SubsystemScoreCalculator(SeverityClassifier severityClassifier) {
        this.severityClassifier = severityClassifier;
    }

    public SubsystemHealthAssessment calculateScore(DegradationOutput output) {
        if (output == null) {
            return new SubsystemHealthAssessment("unknown", SeverityLevel.UNKNOWN, null, 0.0, 1.0, List.of("NO_DATA"));
        }

        String subsystemId = output.getSubsystemId();
        List<String> degradedFlags = new ArrayList<>();
        
        if (output.getDataSufficiencyStatus() == DataSufficiency.INSUFFICIENT) {
            degradedFlags.add("INSUFFICIENT_DATA");
            return new SubsystemHealthAssessment(subsystemId, SeverityLevel.UNKNOWN, null, 0.0, 1.0, degradedFlags);
        }

        SeverityLevel severity = severityClassifier.classify(output);
        
        if (output.getDataSufficiencyStatus() == DataSufficiency.SPARSE) {
            degradedFlags.add("SPARSE_DATA");
        }

        // Base health starts at 100
        double healthScore = 100.0;
        
        double degScore = output.getDegradationScore() != null ? output.getDegradationScore() : 0.0; // 0 to 100
        double eventRisk = output.getEventRiskEstimate() != null ? output.getEventRiskEstimate() : 0.0; // 0 to 1
        double persistence = output.getPersistenceScore() != null ? output.getPersistenceScore() : 0.0; // 0 to 1
        double recurrence = output.getRecurrenceScore() != null ? output.getRecurrenceScore() : 0.0; // 0 to 1

        // Deductions
        double degDeduction = degScore * 0.5; // Max 50 points lost from degradation
        double riskDeduction = eventRisk * 30.0; // Max 30 points lost from event risk
        double persistenceDeduction = persistence * 10.0; // Max 10 points lost
        double recurrenceDeduction = recurrence * 10.0; // Max 10 points lost
        
        healthScore = healthScore - degDeduction - riskDeduction - persistenceDeduction - recurrenceDeduction;
        
        if (healthScore < 0.0) healthScore = 0.0;
        if (healthScore > 100.0) healthScore = 100.0;
        
        // Confidence scaling
        double baseConfidence = output.getConfidence() != null ? output.getConfidence() : 0.5;
        if (output.getDataSufficiencyStatus() == DataSufficiency.SPARSE) {
            baseConfidence *= 0.5; // Penalty for sparse data
        }
        
        double uncertainty = output.getUncertainty() != null ? output.getUncertainty() : 0.5;

        return new SubsystemHealthAssessment(subsystemId, severity, healthScore, baseConfidence, uncertainty, degradedFlags);
    }
}
