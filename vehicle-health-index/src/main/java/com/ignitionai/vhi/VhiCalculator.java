package com.ignitionai.vhi;

import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase6.VehicleHealthBand;
import com.ignitionai.phase6.SeverityLevel;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class VhiCalculator {

    public VehicleHealthAssessment calculateVhi(String vehicleId, Long timestampMs, Long windowMs, 
                                                List<SubsystemHealthAssessment> subsystemAssessments, 
                                                Map<String, Double> componentWeights,
                                                List<String> evidenceReferences, String calculationVersion,
                                                String registryVersion, String contextVersion) {
        
        double weightedSum = 0.0;
        double validWeightSum = 0.0;
        
        for (SubsystemHealthAssessment sub : subsystemAssessments) {
            if (sub.getSeverityLevel() == SeverityLevel.UNKNOWN || sub.getHealthScore() == null) {
                continue; // Skip unknown subsystems
            }
            
            Double weight = componentWeights.getOrDefault(sub.getSubsystemId(), 1.0);
            weightedSum += sub.getHealthScore() * weight;
            validWeightSum += weight;
        }

        Double vhi = null;
        VehicleHealthBand band = VehicleHealthBand.UNKNOWN;

        if (validWeightSum > 0) {
            vhi = weightedSum / validWeightSum;
            
            if (vhi >= 90.0) band = VehicleHealthBand.EXCELLENT;
            else if (vhi >= 75.0) band = VehicleHealthBand.GOOD;
            else if (vhi >= 50.0) band = VehicleHealthBand.FAIR;
            else if (vhi >= 25.0) band = VehicleHealthBand.POOR;
            else band = VehicleHealthBand.CRITICAL;
        }
        
        return new VehicleHealthAssessment(vehicleId, timestampMs, windowMs, vhi, band, 
                                           subsystemAssessments, componentWeights, evidenceReferences, 
                                           calculationVersion, registryVersion, contextVersion);
    }
}
