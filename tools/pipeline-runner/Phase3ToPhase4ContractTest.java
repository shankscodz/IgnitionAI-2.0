package tools.pipeline_runner;

import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.obd.*;

import java.io.File;
import java.util.*;

public class Phase3ToPhase4ContractTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Phase 3 to Phase 4 Contract Test ===");

        File fixture = new File("fixtures/obd-input-v1-multisample.json");
        ObservationStream stream = ObdInputParser.parse(fixture);
        
        assert stream.readings.size() == 35 : "Expected 35 readings from multisample fixture";
        
        VehicleContextManager contextManager = new VehicleContextManager();
        VehicleContext ctxOrig = contextManager.initializeContext(stream.vehicleId, 0L);
        Configuration config = new Configuration(null, null, null, "EA888 Gen3", null, null, null, null, null, null, null);
        VehicleContext context = new VehicleContext("1.0", 0L, ctxOrig.getIdentity(), config, ctxOrig.getOperatingConditions(), ctxOrig.getEvidenceQuality());

        WindowBuffer buffer = new WindowBuffer();
        FeatureCatalogue catalogue = new FeatureCatalogue();
        
        ManifestLoader loader = new ManifestLoader();
        VirtualSensorRuntime runtime = new VirtualSensorRuntime();
        runtime.loadSensors(loader.loadManifests(new File("virtual-sensors/src/main/resources/sensors")));

        Map<String, AnalyticalObservation> rawObs = new HashMap<>();
        Map<String, Double> latestObs = new HashMap<>();
        long currentTs = 0;

        for (SensorReadingDto dto : stream.readings) {
            buffer.addReading(new SensorReading(dto.signalId, dto.value, dto.timestampMs, dto.units));
            latestObs.put(dto.signalId, dto.value);
            currentTs = dto.timestampMs;
            
            rawObs.put(dto.signalId, new AnalyticalObservation(dto.signalId, dto.value, dto.units, currentTs, 
                AnalyticalObservation.QualityState.AVAILABLE, List.of(), context.getContextVersion(), "1.0", "0", "adapter"));
            
            context = contextManager.transitionContext(context, latestObs, currentTs);
        }

        System.out.println("  PASS  Stream Parsing and Context Maintenance");

        Map<String, AnalyticalObservation> allOutputs = new HashMap<>(rawObs);

        for (Feature f : catalogue.getAllFeatures()) {
            AnalyticalObservation val = f.calculate(buffer, context);
            allOutputs.put(f.getFeatureId(), val);
            
            // Contract checks
            assert val.getObservationId() != null;
            assert val.getQualityState() != null;
            assert val.getTimestampMs() != null;
            if (val.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE) {
                assert val.getValue() != null;
                assert val.getUnit() != null;
                assert val.getSourceObservationReferences() != null;
                assert val.getContextVersion() != null;
            }
        }

        System.out.println("  PASS  Direct Feature Phase 4 Contract Conformance");

        for (SensorDefinition def : runtime.getRegisteredSensors().values()) {
            AnalyticalObservation val = runtime.evaluate(def.getSensorId(), allOutputs, context, currentTs);
            
            assert val.getObservationId() != null;
            assert val.getQualityState() != null;
            assert val.getTimestampMs() != null;
            if (val.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE) {
                assert val.getValue() != null;
                assert val.getUnit() != null;
                assert val.getSourceObservationReferences() != null;
                assert val.getContextVersion() != null;
            }
        }
        
        System.out.println("  PASS  Virtual Sensor Phase 4 Contract Conformance");

        // Verify successful output for 5 samples (Mean feature needs 5 samples)
        AnalyticalObservation mean = allOutputs.get("coolant_mean_60s");
        assert mean != null && mean.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE : "Mean should be available after 5 samples";
        
        AnalyticalObservation proxy = runtime.evaluate("thermal_response_proxy", allOutputs, context, currentTs);
        assert proxy != null && proxy.getQualityState() == AnalyticalObservation.QualityState.AVAILABLE : "Thermal proxy should be available";
        assert proxy.getValue() > 0 : "Thermal proxy value should be populated";

        System.out.println("  PASS  Successful Availability Validated");
        System.out.println("\n=== Results: 4 passed, 0 failed ===");
    }
}
