package tools.pipeline_runner;

import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.obd.*;

import java.io.File;
import java.util.*;

public class Phase3PipelineRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Phase 3 Pipeline Runner (Multi-Sample)...");

        // 1. Parse JSON Fixture
        File fixture = new File("fixtures/obd-input-v1-multisample.json");
        ObservationStream stream = ObdInputParser.parse(fixture);
        System.out.println("Parsed " + stream.readings.size() + " readings for vehicle " + stream.vehicleId);

        // 2. Initialize Architecture
        VehicleContextManager contextManager = new VehicleContextManager();
        VehicleContext ctxOrig = contextManager.initializeContext(stream.vehicleId, 0L);
        // Inject configuration to pass applicability check for 'EA888 Gen3'
        Configuration config = new Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
        VehicleContext context = new VehicleContext("1.0", 0L, ctxOrig.getIdentity(), config, ctxOrig.getOperatingConditions(), ctxOrig.getEvidenceQuality());

        WindowBuffer buffer = new WindowBuffer();
        FeatureCatalogue featureCatalogue = new FeatureCatalogue();

        ManifestLoader loader = new ManifestLoader();
        VirtualSensorRuntime sensorRuntime = new VirtualSensorRuntime();
        sensorRuntime.loadSensors(loader.loadManifests(new File("virtual-sensors/src/main/resources/sensors")));

        // 3. Process Stream Incrementally
        Map<String, Double> latestObs = new HashMap<>();
        Map<String, AnalyticalObservation> rawObs = new HashMap<>();
        long currentTs = 0;

        List<AnalyticalObservation> pipelineOutput = new ArrayList<>();

        for (SensorReadingDto dto : stream.readings) {
            // Fill Buffer
            buffer.addReading(new SensorReading(dto.signalId, dto.value, dto.timestampMs, dto.units));
            latestObs.put(dto.signalId, dto.value);
            currentTs = dto.timestampMs;
            
            AnalyticalObservation raw = new AnalyticalObservation(dto.signalId, dto.value, dto.units, currentTs, 
                AnalyticalObservation.QualityState.AVAILABLE, List.of("RAW-" + currentTs), context.getContextVersion(), "1.0", "0", "adapter");
            rawObs.put(dto.signalId, raw);
            
            // Context needs to advance per distinct timestamp, for MVP we advance on every reading which is fine
            context = contextManager.transitionContext(context, latestObs, currentTs);
        }

        System.out.println("Final Operating Regime: " + context.getOperatingConditions().getOperatingRegime());
        System.out.println("Regime Transition History Size: " + context.getOperatingConditions().getTransitionHistory().size());

        // 4. Evaluate Features at the end of the stream
        System.out.println("\n--- Direct Features Output ---");
        Map<String, AnalyticalObservation> featureOutputsForSensors = new HashMap<>(rawObs); // Feed raw into virtual sensors too
        
        for (Feature f : featureCatalogue.getAllFeatures()) {
            AnalyticalObservation val = f.calculate(buffer, context);
            pipelineOutput.add(val);
            System.out.println(f.getName() + ": " + val.getValue() + " [" + val.getQualityState() + "] - " + val.getProvenance());
            if (val.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE) {
                featureOutputsForSensors.put(f.getFeatureId(), val);
            }
        }

        // 5. Evaluate Virtual Sensors
        System.out.println("\n--- Virtual Sensors Output ---");
        for (SensorDefinition def : sensorRuntime.getRegisteredSensors().values()) {
            AnalyticalObservation out = sensorRuntime.evaluate(def.getSensorId(), featureOutputsForSensors, context, currentTs);
            pipelineOutput.add(out);
            System.out.println(def.getDisplayName() + ": " + out.getValue() + " [" + out.getQualityState() + "] - " + out.getProvenance());
        }
        
        System.out.println("\nPipeline execution complete. Generated " + pipelineOutput.size() + " downstream AnalyticalObservations.");
    }
}
