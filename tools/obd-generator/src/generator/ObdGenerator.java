package com.ignitionai.obdgenerator.generator;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdinput.schema.*;
import java.time.Instant;
import java.util.*;

public class ObdGenerator {

    private final GeneratorConfig config;
    private final Random random;

    public ObdGenerator(GeneratorConfig config) {
        this.config = config;
        this.random = new Random(config.getRandomSeed());
    }

    public GenerationResult generate() {
        List<ObdMessage> publicStream = new ArrayList<>();
        List<GroundTruthRecord> groundTruth = new ArrayList<>();
        
        long currentTimeMs = 0;
        Instant startTime = Instant.now();
        String sessionId = "SIM-" + UUID.randomUUID().toString();
        
        // Very basic simulation loop just to satisfy the interface for MVP
        long sequence = 1;
        while (currentTimeMs < config.getDurationMs()) {
            
            List<SensorReading> readings = new ArrayList<>();
            Map<String, Double> trueValues = new HashMap<>();
            
            for (String signal : config.getSupportedSignals()) {
                double rateHz = config.getSamplingRatesHz().getOrDefault(signal, 1.0);
                long intervalMs = (long) (1000.0 / rateHz);
                
                if (currentTimeMs % intervalMs == 0) {
                    double trueValue = 100.0; // base arbitrary value
                    trueValues.put(signal, trueValue);
                    
                    if (random.nextDouble() < config.getMissingnessProbability()) {
                        continue;
                    }
                    
                    double noisedValue = trueValue;
                    
                    if (config.getStuckValues() != null && config.getStuckValues().containsKey(signal)) {
                        noisedValue = config.getStuckValues().get(signal);
                    } else {
                        if (config.getBias() != null && config.getBias().containsKey(signal)) {
                            noisedValue += config.getBias().get(signal);
                        }
                        if (config.getNoiseVariance() != null && config.getNoiseVariance().containsKey(signal)) {
                            noisedValue += random.nextGaussian() * config.getNoiseVariance().get(signal);
                        }
                    }

                    readings.add(SensorReading.builder()
                        .signalId(signal)
                        .value(noisedValue)
                        .unit("unit")
                        .measurementTimeBasis(MeasurementTimeBasis.SIMULATION)
                        .monotonicMs(currentTimeMs)
                        .receivedAt(startTime.plusMillis(currentTimeMs))
                        .quality(new Quality(QualityStatus.VALID, null, 0))
                        .provenance(new SignalProvenance("sim", "ref"))
                        .build());
                }
            }

            List<DtcObservation> dtcs = new ArrayList<>();
            if (config.getInjectedDtcs() != null && !config.getInjectedDtcs().isEmpty()) {
                for (String dtcCode : config.getInjectedDtcs()) {
                     dtcs.add(DtcObservation.builder()
                        .code(dtcCode)
                        .status(DtcStatus.CONFIRMED)
                        .observationType(ObservationType.SNAPSHOT)
                        .observedAt(startTime.plusMillis(currentTimeMs))
                        .monotonicMs(currentTimeMs)
                        .provenance(new DtcProvenance("sim", "ref"))
                        .build());
                }
            }

            if (!readings.isEmpty() || !dtcs.isEmpty()) {
                ObdMessage msg = ObdMessage.builder()
                    .messageId("MSG-" + sequence)
                    .sessionId(sessionId)
                    .sequence(sequence++)
                    .messageType(MessageType.TELEMETRY)
                    .sourceType(SourceType.SIMULATION)
                    .emittedAt(startTime.plusMillis(currentTimeMs))
                    .receivedAt(startTime.plusMillis(currentTimeMs))
                    .vehicleRef(new VehicleRef(config.getVehicleId(), null, IdentityStatus.UNKNOWN))
                    .sensorReadings(readings)
                    .dtcObservations(dtcs)
                    .rawProvenance(new Provenance("generator", "1.0", "ref", config.getRandomSeed()))
                    .build();
                publicStream.add(msg);
                
                groundTruth.add(new GroundTruthRecord(currentTimeMs, trueValues, config.getInjectedDtcs()));
            }

            currentTimeMs += 10; // 10ms simulation tick
        }

        return new GenerationResult(publicStream, groundTruth);
    }
}
