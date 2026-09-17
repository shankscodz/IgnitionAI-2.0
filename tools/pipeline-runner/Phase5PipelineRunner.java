package tools.pipeline_runner;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.degradation.store.DegradationEvidenceStore;
import com.ignitionai.degradation.store.InMemoryEvidenceStore;
import com.ignitionai.degradation.features.DegradationFeatureBuilder;
import com.ignitionai.degradation.features.DegradationFeatures;
import com.ignitionai.degradation.analyzer.TrendAnalyzer;
import com.ignitionai.degradation.analyzer.TrendResult;
import com.ignitionai.degradation.state.DegradationStateEstimator;
import com.ignitionai.degradation.state.DegradationStateResult;
import com.ignitionai.degradation.risk.EventRiskEstimator;
import com.ignitionai.degradation.risk.EventRiskResult;

import java.io.File;
import java.util.List;

public class Phase5PipelineRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Phase 5 Pipeline Runner (Degradation State & Event Risk)...");

        // 1. Parse Phase 4 JSON Fixture
        File fixture = new File("fixtures/phase4-anomaly-episodes.json");
        List<AnomalyEpisode> stream = AnomalyEpisodeParser.parse(fixture);
        System.out.println("Parsed " + stream.size() + " incoming anomaly episodes from Phase 4.\n");

        // 2. Initialize Architecture
        DegradationEvidenceStore store = new InMemoryEvidenceStore();
        DegradationFeatureBuilder featureBuilder = new DegradationFeatureBuilder();
        TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
        DegradationStateEstimator stateEstimator = new DegradationStateEstimator();
        EventRiskEstimator riskEstimator = new EventRiskEstimator();

        // 3. Process Stream Incrementally
        for (AnomalyEpisode episode : stream) {
            System.out.println("--- Processing Episode: " + episode.getEpisodeId() + " for Vehicle: " + episode.getVehicleId() + " ---");
            
            // Retrieve History
            List<AnomalyEpisode> history = store.getEpisodes(episode.getVehicleId(), episode.getSubsystemId());
            
            // Build Features
            DegradationFeatures features = featureBuilder.buildFeatures(episode, history);
            
            // Analyze Trend
            TrendResult trend = trendAnalyzer.analyzeTrend(episode, history);
            
            // Estimate State
            DegradationStateResult stateResult = stateEstimator.estimateState(features, trend);
            
            // Estimate Risk
            String endpoint = "critical_failure";
            long horizonHours = 168; // 7 days
            boolean isCalibrated = true; // Assume true for demo, can be parsed from episode
            
            // Check if sparse history example should be explicitly uncalibrated
            if (episode.getVehicleId().contains("SPARSE")) {
                isCalibrated = false;
            }

            EventRiskResult riskResult = riskEstimator.estimateRisk(features, trend, stateResult, endpoint, horizonHours, isCalibrated);
            
            // Store Current Episode (for future history)
            store.storeEpisode(episode);
            
            // Calculate Observed Exposure
            double observedExposureMs = 0.0;
            for (AnomalyEpisode ep : history) {
                observedExposureMs += ep.getDurationMs() != null ? ep.getDurationMs() : 0.0;
            }
            observedExposureMs += episode.getDurationMs() != null ? episode.getDurationMs() : 0.0;
            double observedExposureHours = observedExposureMs / 3600000.0;

            // Construct Output Contract
            DegradationOutput output = new DegradationOutput();
            output.setVehicleId(episode.getVehicleId());
            output.setSubsystemId(episode.getSubsystemId());
            output.setEvaluationTimeMs(episode.getEndTimeMs());
            output.setDegradationState(stateResult.getState());
            output.setDegradationScore(stateResult.getScore());
            output.setTrendDirection(trend.getDirection());
            output.setTrendSlope(trend.getTrendSlope());
            output.setPersistenceScore(features.getMeanPersistence());
            output.setRecurrenceScore(features.getRecurrenceCount().doubleValue());
            output.setTimeSincePreviousEventMs(features.getTimeSinceLastAnomalyMs());
            output.setEventRiskEstimate(riskResult.getRiskEstimate());
            output.setPredictionHorizonMs(riskResult.getPredictionHorizonMs());
            output.setConfidence(riskResult.getConfidence());
            output.setUncertainty(riskResult.getUncertainty());
            output.setEvidenceReferences(episode.getEvidenceReferences());
            output.setModelVersion("1.0");
            output.setConfigurationVersion("1.0");
            
            // Set New Phase 5 advanced fields
            output.setEndpointLabel(endpoint);
            output.setObservedExposure(observedExposureHours);
            output.setExposureUnits("hours");
            output.setRequiredHistory(2);
            
            if (features.getRecurrenceCount() < 2) {
                output.setDataSufficiencyStatus(DegradationOutput.DataSufficiency.SPARSE);
            } else {
                output.setDataSufficiencyStatus(DegradationOutput.DataSufficiency.SUFFICIENT);
            }
            
            output.setIsCensored(false);
            output.setTimeOriginMs(episode.getStartTimeMs()); // Simplified time origin
            output.setRiskStatus(riskResult.getStatus());

            System.out.println(" State: " + output.getDegradationState() + " (Score: " + String.format("%.2f", output.getDegradationScore()) + ")");
            System.out.println(" Trend: " + output.getTrendDirection() + " (Slope: " + String.format("%.4f", output.getTrendSlope()) + ")");
            System.out.println(" Risk:  " + String.format("%.1f%%", output.getEventRiskEstimate() * 100) + " (Confidence: " + String.format("%.2f", output.getConfidence()) + ", Uncertainty: " + String.format("%.2f", output.getUncertainty()) + ", Status: " + output.getRiskStatus() + ", Data: " + output.getDataSufficiencyStatus() + ")\n");
        }

        System.out.println("Phase 5 pipeline execution complete.");
    }
}
