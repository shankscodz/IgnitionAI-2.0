package com.ignitionai.obdinput.preprocessor;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obdinput.schema.SensorReading;
import com.ignitionai.obdinput.schema.DtcObservation;
import com.ignitionai.obdinput.schema.ObservationType;
import com.ignitionai.obdinput.schema.QualityStatus;
import com.ignitionai.obdinput.schema.Quality;
import java.util.*;

public class ObdPreProcessor {

    private long lastSequence = -1;
    // Map of signal_id to last monotonic_ms
    private final Map<String, Long> lastMonotonicTimes = new HashMap<>();
    // To track duplicates (session, signal, ecu, time)
    private final Set<String> seenSensorReadings = new HashSet<>();
    // To track active DTCs for session (to support snapshot clearing logic)
    private final Set<String> knownSessionDtcs = new HashSet<>();

    public ObdMessage process(ObdMessage message) {
        validateSchemaAndRequiredFields(message);
        
        long currentSeq = message.getSequence();
        if (lastSequence != -1 && currentSeq != lastSequence + 1) {
            // Sequence gap detected. We log it conceptually, not silently repair.
            System.err.println("Sequence gap detected. Expected " + (lastSequence + 1) + ", got " + currentSeq);
        }
        lastSequence = currentSeq;

        List<SensorReading> processedReadings = new ArrayList<>();
        for (SensorReading reading : message.getSensorReadings()) {
            // 8. Never interpolate at input boundary. Interpolated field must be false.
            if (reading.getQuality().isInterpolated()) {
                throw new ValidationException("Interpolated data is not allowed at input boundary.");
            }
            
            // Generate duplicate check key
            String key = String.format("%s-%s-%s-%d-%f", 
                message.getSessionId(), 
                reading.getSignalId(), 
                reading.getEcuId(), 
                reading.getMonotonicMs(),
                reading.getValue());

            String duplicateCheckKey = String.format("%s-%s-%s-%d", 
                message.getSessionId(), 
                reading.getSignalId(), 
                reading.getEcuId(), 
                reading.getMonotonicMs());

            if (seenSensorReadings.contains(key)) {
                // Exact repeat - deduplicate (drop)
                continue;
            } else if (seenSensorReadings.contains(duplicateCheckKey)) {
                // Conflicting duplicate (same time/signal, different value) - preserve and flag
                processedReadings.add(flagAsConflicting(reading));
                continue;
            }
            seenSensorReadings.add(key);
            seenSensorReadings.add(duplicateCheckKey);

            // Out of order detection
            long lastTime = lastMonotonicTimes.getOrDefault(reading.getSignalId(), -1L);
            if (reading.getMonotonicMs() < lastTime) {
                processedReadings.add(flagAsOutOfOrder(reading));
            } else {
                processedReadings.add(reading);
            }
            lastMonotonicTimes.put(reading.getSignalId(), reading.getMonotonicMs());
        }

        List<DtcObservation> processedDtcs = new ArrayList<>();
        boolean isCompleteSnapshot = false;

        for (DtcObservation dtc : message.getDtcObservations()) {
            processedDtcs.add(dtc);
            if (dtc.getObservationType() == ObservationType.SNAPSHOT) {
                isCompleteSnapshot = true;
                knownSessionDtcs.add(dtc.getCode());
            } else if (dtc.getObservationType() == ObservationType.ADDED) {
                knownSessionDtcs.add(dtc.getCode());
            } else if (dtc.getObservationType() == ObservationType.CLEARED) {
                knownSessionDtcs.remove(dtc.getCode());
            }
        }
        
        // A complete DTC snapshot must be distinguished from an incremental update.
        // An empty incremental update must never clear previously observed DTCs.
        // If it's a complete snapshot, and no DTCs are present, it implicitly clears them.
        if (isCompleteSnapshot && message.getDtcObservations().isEmpty()) {
            knownSessionDtcs.clear();
        }

        return ObdMessage.builder()
            .messageId(message.getMessageId())
            .sessionId(message.getSessionId())
            .sequence(message.getSequence())
            .messageType(message.getMessageType())
            .sourceType(message.getSourceType())
            .emittedAt(message.getEmittedAt())
            .receivedAt(message.getReceivedAt())
            .vehicleRef(message.getVehicleRef())
            .sensorReadings(processedReadings)
            .dtcObservations(processedDtcs)
            .capabilitySnapshot(message.getCapabilitySnapshot())
            .rawProvenance(message.getRawProvenance())
            .build();
    }

    private void validateSchemaAndRequiredFields(ObdMessage message) {
        if (!"obd-input.v1".equals(message.getSchemaVersion())) {
            throw new ValidationException("Unsupported schema version: " + message.getSchemaVersion());
        }
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            throw new ValidationException("message_id is required");
        }
        if (message.getSessionId() == null || message.getSessionId().isEmpty()) {
            throw new ValidationException("session_id is required");
        }
        for (SensorReading reading : message.getSensorReadings()) {
            if (reading.getSignalId() == null || reading.getSignalId().isEmpty()) {
                throw new ValidationException("signal_id is required");
            }
            if (reading.getUnit() == null || reading.getUnit().isEmpty()) {
                throw new ValidationException("unit is required");
            }
        }
    }

    private SensorReading flagAsConflicting(SensorReading reading) {
        // Conceptually flag quality as INVALID due to conflict.
        Quality newQuality = new Quality(QualityStatus.INVALID, "CONFLICTING_DUPLICATE", reading.getQuality().getAgeMs());
        return copyWithNewQuality(reading, newQuality);
    }

    private SensorReading flagAsOutOfOrder(SensorReading reading) {
        // Flag quality as STALE due to being out-of-order
        Quality newQuality = new Quality(QualityStatus.STALE, "OUT_OF_ORDER", reading.getQuality().getAgeMs());
        return copyWithNewQuality(reading, newQuality);
    }

    private SensorReading copyWithNewQuality(SensorReading r, Quality q) {
        return SensorReading.builder()
            .signalId(r.getSignalId())
            .pidOrDid(r.getPidOrDid())
            .ecuId(r.getEcuId())
            .value(r.getValue())
            .unit(r.getUnit())
            .measuredAt(r.getMeasuredAt())
            .measurementTimeBasis(r.getMeasurementTimeBasis())
            .monotonicMs(r.getMonotonicMs())
            .receivedAt(r.getReceivedAt())
            .quality(q)
            .provenance(r.getProvenance())
            .build();
    }
}
