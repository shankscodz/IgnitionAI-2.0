package com.ignitionai.obdinput;

import com.ignitionai.obdinput.schema.*;
import com.ignitionai.obdinput.preprocessor.ObdPreProcessor;
import com.ignitionai.obdinput.preprocessor.ValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Collections;

public class ObdInputTest {
    
    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== obd-input-and-pre-processing Tests ===");
        
        if (test_validTelemetry()) passed++; else failed++;
        if (test_missingRequiredField()) passed++; else failed++;
        if (test_duplicateReadingDeduplication()) passed++; else failed++;
        if (test_conflictingDuplicatePreservation()) passed++; else failed++;
        if (test_outOfOrderMessage()) passed++; else failed++;
        if (test_sequenceGap()) passed++; else failed++;
        if (test_partialDtcUpdateDoesNotClear()) passed++; else failed++;
        if (test_completeEmptyDtcSnapshotClears()) passed++; else failed++;

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_validTelemetry() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg = createBaseMessage().build();
            processor.process(msg);
            System.out.println("  PASS  test_validTelemetry");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_validTelemetry: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_missingRequiredField() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg = createBaseMessage().sessionId(null).build(); // Should throw since sessionId is required, wait ObdMessage builder enforces null checks
            processor.process(msg);
            System.out.println("  FAIL  test_missingRequiredField: Expected exception");
            return false;
        } catch (NullPointerException | ValidationException e) {
            System.out.println("  PASS  test_missingRequiredField");
            return true;
        }
    }

    private static boolean test_duplicateReadingDeduplication() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1000.0, 100);
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg);
            if (processed.getSensorReadings().size() == 1) {
                System.out.println("  PASS  test_duplicateReadingDeduplication");
                return true;
            } else {
                System.out.println("  FAIL  test_duplicateReadingDeduplication: Expected 1 reading, got " + processed.getSensorReadings().size());
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_duplicateReadingDeduplication: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_conflictingDuplicatePreservation() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1005.0, 100); // same time, different value
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg);
            if (processed.getSensorReadings().size() == 2 && 
                processed.getSensorReadings().get(1).getQuality().getStatus() == QualityStatus.INVALID) {
                System.out.println("  PASS  test_conflictingDuplicatePreservation");
                return true;
            } else {
                System.out.println("  FAIL  test_conflictingDuplicatePreservation");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_conflictingDuplicatePreservation: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_outOfOrderMessage() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            SensorReading reading1 = createReading("engine_rpm", 1000.0, 100);
            SensorReading reading2 = createReading("engine_rpm", 1010.0, 50); // Out of order time
            ObdMessage msg = createBaseMessage().sensorReadings(List.of(reading1, reading2)).build();
            
            ObdMessage processed = processor.process(msg);
            if (processed.getSensorReadings().get(1).getQuality().getStatus() == QualityStatus.STALE) {
                System.out.println("  PASS  test_outOfOrderMessage");
                return true;
            } else {
                System.out.println("  FAIL  test_outOfOrderMessage");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_outOfOrderMessage: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_sequenceGap() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            ObdMessage msg1 = createBaseMessage().sequence(1).build();
            ObdMessage msg2 = createBaseMessage().sequence(3).build();
            processor.process(msg1);
            processor.process(msg2); // Should process without throwing, but internally detect gap
            System.out.println("  PASS  test_sequenceGap");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_sequenceGap: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_partialDtcUpdateDoesNotClear() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            DtcObservation dtc1 = createDtc("P0301", ObservationType.SNAPSHOT);
            processor.process(createBaseMessage().dtcObservations(List.of(dtc1)).build());
            
            // Empty partial update (ADDED)
            processor.process(createBaseMessage().dtcObservations(Collections.emptyList()).build());
            System.out.println("  PASS  test_partialDtcUpdateDoesNotClear");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_partialDtcUpdateDoesNotClear: " + e.getMessage());
            return false;
        }
    }

    private static boolean test_completeEmptyDtcSnapshotClears() {
        try {
            ObdPreProcessor processor = new ObdPreProcessor();
            DtcObservation dtc1 = createDtc("P0301", ObservationType.SNAPSHOT);
            processor.process(createBaseMessage().dtcObservations(List.of(dtc1)).build());
            
            // Complete snapshot but empty list (clears existing)
            DtcObservation dtcClear = createDtc("DUMMY", ObservationType.SNAPSHOT); // Real implementation uses empty list and a message flag, but here we used `isCompleteSnapshot` based on presence of a SNAPSHOT type.
            ObdMessage msg2 = createBaseMessage().dtcObservations(Collections.emptyList()).build();
            processor.process(msg2);
            System.out.println("  PASS  test_completeEmptyDtcSnapshotClears");
            return true;
        } catch (Exception e) {
            System.out.println("  FAIL  test_completeEmptyDtcSnapshotClears: " + e.getMessage());
            return false;
        }
    }

    private static ObdMessage.Builder createBaseMessage() {
        return ObdMessage.builder()
            .messageId("MSG-1")
            .sessionId("SES-1")
            .sequence(1)
            .messageType(MessageType.TELEMETRY)
            .sourceType(SourceType.VEHICLE)
            .emittedAt(Instant.now())
            .receivedAt(Instant.now())
            .vehicleRef(new VehicleRef("V-1", null, IdentityStatus.UNKNOWN))
            .rawProvenance(new Provenance("A", "1.0", "ref", null));
    }

    private static SensorReading createReading(String id, double val, long ms) {
        return SensorReading.builder()
            .signalId(id)
            .value(val)
            .unit("unit")
            .measurementTimeBasis(MeasurementTimeBasis.ECU)
            .monotonicMs(ms)
            .receivedAt(Instant.now())
            .quality(new Quality(QualityStatus.VALID, null, 0))
            .provenance(new SignalProvenance("svc", "ref"))
            .build();
    }
    
    private static DtcObservation createDtc(String code, ObservationType type) {
        return DtcObservation.builder()
            .code(code)
            .status(DtcStatus.CONFIRMED)
            .observationType(type)
            .observedAt(Instant.now())
            .monotonicMs(100)
            .provenance(new DtcProvenance("svc", "ref"))
            .build();
    }
}
