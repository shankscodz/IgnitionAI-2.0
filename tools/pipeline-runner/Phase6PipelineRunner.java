package tools.pipeline_runner;

import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase5.DegradationOutput.DegradationState;
import com.ignitionai.phase5.DegradationOutput.DataSufficiency;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import com.ignitionai.phase6.SubsystemHealthAssessment;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase6.VehicleHealthBand;
import com.ignitionai.phase6.SeverityLevel;

import com.ignitionai.severity.SeverityClassifier;
import com.ignitionai.severity.SeverityConfiguration;
import com.ignitionai.healthscore.SubsystemScoreCalculator;
import com.ignitionai.vhi.VhiCalculator;
import com.ignitionai.healthevidence.HealthEvidenceStore;
import com.ignitionai.healthevidence.HealthEvidenceRecord;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class Phase6PipelineRunner {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        
        System.out.println("=== Phase 6 End-to-End Tests ===");
        
        if (test_healthyVehicle()) passed++; else failed++;
        if (test_mildlyDegraded()) passed++; else failed++;
        if (test_multipleDegradedSubsystems()) passed++; else failed++;
        if (test_criticalSubsystem()) passed++; else failed++;
        if (test_missingSubsystemData()) passed++; else failed++;
        if (test_lowConfidenceOutput()) passed++; else failed++;
        if (test_recoveryAfterImprovement()) passed++; else failed++;
        
        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static DegradationOutput createMockOutput(String subsystemId, DegradationState state, 
                                                      Double degScore, Double risk, Double confidence, 
                                                      DataSufficiency sufficiency) {
        DegradationOutput output = new DegradationOutput();
        output.setSubsystemId(subsystemId);
        output.setDegradationState(state);
        output.setDegradationScore(degScore);
        output.setEventRiskEstimate(risk);
        output.setConfidence(confidence);
        output.setDataSufficiencyStatus(sufficiency);
        return output;
    }

    private static boolean test_healthyVehicle() {
        try {
            System.out.println("Running test_healthyVehicle...");
            SeverityClassifier classifier = new SeverityClassifier(null);
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(classifier);
            VhiCalculator vhiCalc = new VhiCalculator();
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.HEALTHY, 0.0, 0.0, 1.0, DataSufficiency.SUFFICIENT);
            DegradationOutput trans = createMockOutput("transmission", DegradationState.HEALTHY, 0.0, 0.0, 1.0, DataSufficiency.SUFFICIENT);
            
            List<SubsystemHealthAssessment> assessments = List.of(
                calc.calculateScore(engine),
                calc.calculateScore(trans)
            );
            
            Map<String, Double> weights = Map.of("engine", 0.6, "transmission", 0.4);
            
            VehicleHealthAssessment vha = vhiCalc.calculateVhi("VEH-1", System.currentTimeMillis(), 3600000L, 
                                                               assessments, weights, new ArrayList<>(), "1.0", "1.0", "1.0");
            
            if (vha.getVehicleHealthBand() != VehicleHealthBand.EXCELLENT) throw new Exception("Expected EXCELLENT band");
            if (vha.getVehicleHealthIndex() != 100.0) throw new Exception("Expected 100.0 VHI");
            
            System.out.println("  PASS  test_healthyVehicle");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_healthyVehicle: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_mildlyDegraded() {
        try {
            System.out.println("Running test_mildlyDegraded...");
            SeverityClassifier classifier = new SeverityClassifier(null);
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(classifier);
            VhiCalculator vhiCalc = new VhiCalculator();
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.EARLY_DEGRADATION, 25.0, 0.1, 0.9, DataSufficiency.SUFFICIENT);
            DegradationOutput trans = createMockOutput("transmission", DegradationState.HEALTHY, 0.0, 0.0, 1.0, DataSufficiency.SUFFICIENT);
            
            List<SubsystemHealthAssessment> assessments = List.of(
                calc.calculateScore(engine),
                calc.calculateScore(trans)
            );
            
            if (assessments.get(0).getSeverityLevel() != SeverityLevel.WATCH) throw new Exception("Expected WATCH severity");
            
            Map<String, Double> weights = Map.of("engine", 0.6, "transmission", 0.4);
            VehicleHealthAssessment vha = vhiCalc.calculateVhi("VEH-2", System.currentTimeMillis(), 3600000L, 
                                                               assessments, weights, new ArrayList<>(), "1.0", "1.0", "1.0");
            
            // Engine loses 25 * 0.5 = 12.5 points, Risk loses 0.1 * 30 = 3 points. Total 84.5
            // VHI = 84.5 * 0.6 + 100 * 0.4 = 50.7 + 40 = 90.7
            if (vha.getVehicleHealthIndex() < 90.0 || vha.getVehicleHealthIndex() > 91.0) throw new Exception("VHI calculation incorrect");
            
            System.out.println("  PASS  test_mildlyDegraded");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_mildlyDegraded: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_multipleDegradedSubsystems() {
        try {
            System.out.println("Running test_multipleDegradedSubsystems...");
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(new SeverityClassifier(null));
            VhiCalculator vhiCalc = new VhiCalculator();
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.WORSENING_DEGRADATION, 60.0, 0.6, 0.8, DataSufficiency.SUFFICIENT);
            DegradationOutput trans = createMockOutput("transmission", DegradationState.STABLE_DEGRADATION, 55.0, 0.4, 0.9, DataSufficiency.SUFFICIENT);
            
            List<SubsystemHealthAssessment> assessments = List.of(calc.calculateScore(engine), calc.calculateScore(trans));
            
            Map<String, Double> weights = Map.of("engine", 0.5, "transmission", 0.5);
            VehicleHealthAssessment vha = vhiCalc.calculateVhi("VEH-3", System.currentTimeMillis(), 3600000L, 
                                                               assessments, weights, new ArrayList<>(), "1.0", "1.0", "1.0");
            
            if (vha.getVehicleHealthBand() == VehicleHealthBand.EXCELLENT) throw new Exception("Expected non-excellent band");
            
            System.out.println("  PASS  test_multipleDegradedSubsystems");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_multipleDegradedSubsystems: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_criticalSubsystem() {
        try {
            System.out.println("Running test_criticalSubsystem...");
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(new SeverityClassifier(null));
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.WORSENING_DEGRADATION, 85.0, 0.9, 0.95, DataSufficiency.SUFFICIENT);
            SubsystemHealthAssessment assmt = calc.calculateScore(engine);
            
            if (assmt.getSeverityLevel() != SeverityLevel.CRITICAL) throw new Exception("Expected CRITICAL severity");
            
            System.out.println("  PASS  test_criticalSubsystem");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_criticalSubsystem: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_missingSubsystemData() {
        try {
            System.out.println("Running test_missingSubsystemData...");
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(new SeverityClassifier(null));
            VhiCalculator vhiCalc = new VhiCalculator();
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.HEALTHY, 0.0, 0.0, 1.0, DataSufficiency.SUFFICIENT);
            DegradationOutput trans = createMockOutput("transmission", DegradationState.INSUFFICIENT_HISTORY, null, null, null, DataSufficiency.INSUFFICIENT);
            
            List<SubsystemHealthAssessment> assessments = List.of(calc.calculateScore(engine), calc.calculateScore(trans));
            
            if (assessments.get(1).getSeverityLevel() != SeverityLevel.UNKNOWN) throw new Exception("Expected UNKNOWN severity for missing data");
            
            Map<String, Double> weights = Map.of("engine", 0.6, "transmission", 0.4);
            VehicleHealthAssessment vha = vhiCalc.calculateVhi("VEH-4", System.currentTimeMillis(), 3600000L, 
                                                               assessments, weights, new ArrayList<>(), "1.0", "1.0", "1.0");
            
            if (vha.getVehicleHealthIndex() != 100.0) throw new Exception("VHI should ignore UNKNOWN transmission weight and scale engine 100% to 100.0");
            
            System.out.println("  PASS  test_missingSubsystemData");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_missingSubsystemData: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_lowConfidenceOutput() {
        try {
            System.out.println("Running test_lowConfidenceOutput...");
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(new SeverityClassifier(null));
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.STABLE_DEGRADATION, 40.0, 0.2, 0.8, DataSufficiency.SPARSE);
            SubsystemHealthAssessment assmt = calc.calculateScore(engine);
            
            if (!assmt.getDegradedDataFlags().contains("SPARSE_DATA")) throw new Exception("Expected SPARSE_DATA flag");
            if (assmt.getConfidence() >= 0.8) throw new Exception("Confidence should be penalized for sparse data");
            
            System.out.println("  PASS  test_lowConfidenceOutput");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_lowConfidenceOutput: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_recoveryAfterImprovement() {
        try {
            System.out.println("Running test_recoveryAfterImprovement...");
            SubsystemScoreCalculator calc = new SubsystemScoreCalculator(new SeverityClassifier(null));
            
            DegradationOutput engine = createMockOutput("engine", DegradationState.RECOVERED_DEGRADATION, 5.0, 0.05, 0.9, DataSufficiency.SUFFICIENT);
            SubsystemHealthAssessment assmt = calc.calculateScore(engine);
            
            if (assmt.getSeverityLevel() != SeverityLevel.NORMAL) throw new Exception("Expected NORMAL severity for recovered");
            
            System.out.println("  PASS  test_recoveryAfterImprovement");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_recoveryAfterImprovement: " + e.getMessage());
            return false;
        }
    }
}
