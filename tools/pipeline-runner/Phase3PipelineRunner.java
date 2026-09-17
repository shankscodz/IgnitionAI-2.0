package tools.pipeline_runner;

import com.ignitionai.context.*;
import com.ignitionai.features.*;
import com.ignitionai.features.impl.*;
import com.ignitionai.virtualsensors.*;
import com.ignitionai.obd.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Phase3PipelineRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Phase 3 Pipeline Runner...");

        // 1. Mock JSON Parsing from Fixture (Manual parsing for MVP)
        File fixture = new File("fixtures/obd-input-v1-sample.json");
        ObservationStream stream = parseFixture(fixture);
        System.out.println("Parsed " + stream.readings.size() + " readings for vehicle " + stream.vehicleId);

        // 2. Initialize Architecture
        VehicleContextManager contextManager = new VehicleContextManager();
        VehicleContext context = contextManager.initializeContext(stream.vehicleId, 0L);
        WindowBuffer buffer = new WindowBuffer();

        List<Feature> directFeatures = List.of(
            new CoolantWarmupRateFeature(),
            new IdleRpmStabilityFeature(),
            new MissingDataFractionFeature(),
            new SpeedMpsFeature(),
            new WindowedStatisticFeature() // Coolant mean
        );

        ManifestLoader loader = new ManifestLoader();
        VirtualSensorRuntime sensorRuntime = new VirtualSensorRuntime();
        sensorRuntime.loadSensors(loader.loadManifests(new File("virtual-sensors/src/main/resources/sensors")));

        // 3. Process Stream
        Map<String, Double> latestObs = new HashMap<>();
        long currentTs = 0;

        for (SensorReadingDto dto : stream.readings) {
            // Fill Buffer
            buffer.addReading(new SensorReading(dto.signalId, dto.value, dto.timestampMs, dto.units));
            latestObs.put(dto.signalId, dto.value);
            currentTs = dto.timestampMs;
            
            // Advance Context
            context = contextManager.transitionContext(context, latestObs, currentTs);
        }

        System.out.println("Final Operating Regime: " + context.getOperatingConditions().getOperatingRegime());
        System.out.println("Regime Transition History Size: " + context.getOperatingConditions().getTransitionHistory().size());

        // 4. Evaluate Features
        System.out.println("\n--- Direct Features Output ---");
        Map<String, SensorReading> featureOutputsForSensors = new HashMap<>();
        
        for (Feature f : directFeatures) {
            FeatureValue val = f.calculate(buffer, context);
            System.out.println(f.getName() + ": " + val.getValue() + " [" + val.getStatus() + "]");
            if (val.getStatus() == FeatureStatus.AVAILABLE) {
                featureOutputsForSensors.put(f.getFeatureId(), new SensorReading(f.getFeatureId(), val.getValue(), currentTs, val.getUnits()));
            }
        }

        // Add raw signals to virtual sensor input map
        for (Map.Entry<String, Double> entry : latestObs.entrySet()) {
            featureOutputsForSensors.put(entry.getKey(), new SensorReading(entry.getKey(), entry.getValue(), currentTs, "U"));
        }

        // 5. Evaluate Virtual Sensors
        System.out.println("\n--- Virtual Sensors Output ---");
        for (SensorDefinition def : sensorRuntime.getRegisteredSensors().values()) {
            SensorOutput out = sensorRuntime.evaluate(def.getSensorId(), featureOutputsForSensors, context);
            System.out.println(def.getDisplayName() + ": " + out.getValue() + " [" + out.getStatus() + "]");
            if (out.getReason() != null && !out.getReason().equals("success")) {
                System.out.println("  Reason: " + out.getReason());
            }
        }
        
        System.out.println("\nPipeline execution complete.");
    }

    private static ObservationStream parseFixture(File file) throws Exception {
        ObservationStream stream = new ObservationStream();
        stream.readings = new ArrayList<>();
        stream.vehicleId = "VIN1234567890ABCD"; // Hardcoded for manual parse MVP

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String currentSignal = null;
            Double currentVal = null;
            Long currentTs = null;
            
            while ((line = br.readLine()) != null) {
                if (line.contains("\"signal_id\"")) currentSignal = extractString(line);
                if (line.contains("\"value\"")) currentVal = extractDouble(line);
                if (line.contains("\"monotonic_ms\"")) currentTs = extractLong(line);
                
                if (currentSignal != null && currentVal != null && currentTs != null) {
                    SensorReadingDto dto = new SensorReadingDto();
                    dto.signalId = currentSignal;
                    dto.value = currentVal;
                    dto.timestampMs = currentTs;
                    dto.units = "U";
                    stream.readings.add(dto);
                    
                    currentSignal = null;
                    currentVal = null;
                    currentTs = null;
                }
            }
        }
        // Ensure chronological order
        stream.readings.sort(Comparator.comparing(r -> r.timestampMs));
        return stream;
    }
    
    private static String extractString(String line) {
        String[] parts = line.split(":");
        if (parts.length > 1) {
            return parts[1].replaceAll("\"", "").replaceAll(",", "").trim();
        }
        return null;
    }
    
    private static Double extractDouble(String line) {
        String[] parts = line.split(":");
        if (parts.length > 1) {
            try { return Double.parseDouble(parts[1].replaceAll(",", "").trim()); } catch(Exception e) {}
        }
        return null;
    }
    
    private static Long extractLong(String line) {
        String[] parts = line.split(":");
        if (parts.length > 1) {
            try { return Long.parseLong(parts[1].replaceAll(",", "").trim()); } catch(Exception e) {}
        }
        return null;
    }
}
