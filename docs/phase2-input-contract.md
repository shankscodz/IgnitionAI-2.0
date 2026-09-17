# Phase 2 OBD input contract

Status: **frozen transport contract — v1.0; vehicle adapter policy recorded 17 September 2026**

This contract is the boundary between any OBD producer and the IgnitionAI analytical pipeline. A configurable simulator, a replayed file and a real OBD adapter must all emit the same normalized messages. Downstream modules must not know whether a message came from a simulator, a replay or a vehicle.

This document freezes the transport-neutral meaning of the data. It does not freeze a specific dongle, CAN frame format, Bluetooth protocol or polling library.

## 1. What the module accepts

The module accepts one versioned `ObdMessage` at a time. Each message may contain either or both of two internal payloads:

- `sensor_readings`: timestamped measurements.
- `dtc_observations`: diagnostic-code observations.

The message may also carry capability and session metadata. A message with neither payload is invalid unless its `message_type` is a session/capability event defined by the adapter. The analytical pipeline consumes normalized values; raw transport bytes are retained as provenance but are never used as a substitute for the normalized fields.

## 2. Frozen envelope

```json
{
  "schema_version": "obd-input.v1",
  "message_id": "MSG-00000001",
  "session_id": "SES-20260917-0001",
  "sequence": 1,
  "message_type": "telemetry",
  "source_type": "simulation",
  "emitted_at": "2026-09-17T10:00:00.250Z",
  "received_at": "2026-09-17T10:00:00.258Z",
  "vehicle_ref": {
    "vehicle_id": "VEH-TATA-NEXON-0001",
    "vin": null,
    "identity_status": "unknown"
  },
  "sensor_readings": [],
  "dtc_observations": [],
  "capability_snapshot": null,
  "raw_provenance": {
    "adapter_id": "generator-v1",
    "adapter_version": "1.0.0",
    "raw_record_ref": "raw/SES-20260917-0001/00000001.json",
    "simulation_seed": 12345
  }
}
```

### Required envelope fields

| Field | Meaning |
|---|---|
| `schema_version` | Contract version. Consumers reject unsupported major versions. |
| `message_id` | Unique message ID within the persisted system. |
| `session_id` | One capture/inspection/replay session. |
| `sequence` | Monotonically increasing producer sequence where available. Gaps are recorded, not silently repaired. |
| `message_type` | `telemetry`, `dtc_snapshot`, `capability_snapshot`, `session_started`, `session_ended` or `connection_event`. |
| `source_type` | `simulation`, `replay` or `vehicle`. |
| `emitted_at` | Producer wall-clock time in UTC. |
| `received_at` | Time accepted by the input module in UTC. |
| `vehicle_ref` | Internal vehicle reference and identity evidence. VIN may be unavailable. |
| `sensor_readings` | Array of normalized readings; empty is allowed for a DTC-only message. |
| `dtc_observations` | Array of DTC events/observations; empty is allowed for telemetry-only messages. |
| `raw_provenance` | Reference to the original adapter record and version. Required for replay and vehicle data; simulator seed is required for simulation. |

`emitted_at` and `received_at` are not interchangeable. Processing latency is `received_at - emitted_at` when clocks are comparable. Each reading also carries a monotonic elapsed time for analysis that must not depend on wall-clock corrections.

## 3. Sensor reading schema

```json
{
  "signal_id": "engine_rpm",
  "pid_or_did": "0C",
  "ecu_id": "ECM",
  "value": 812.0,
  "unit": "rpm",
  "measured_at": null,
  "measurement_time_basis": "phone_receive",
  "monotonic_ms": 1250,
  "received_at": "2026-09-17T10:00:00.258Z",
  "quality": {
    "status": "valid",
    "raw_quality": "response",
    "age_ms": 8,
    "interpolated": false
  },
  "provenance": {
    "request_service": "01",
    "response_reference": "raw/SES-20260917-0001/00000001.json"
  }
}
```

Rules:

- `signal_id` is the canonical IgnitionAI identifier. PID/DID is optional because enhanced/manufacturer-specific signals may not use a standard PID.
- `value` is numeric and already converted to the declared canonical `unit`. A raw ECU byte response belongs in provenance.
- A boolean or categorical signal uses an explicit categorical field in a later contract extension; do not encode it as an unexplained number in v1.
- `measured_at` is the ECU measurement time when the adapter supplies it. For the Bluetooth MVP it is normally `null`.
- `measurement_time_basis` is `ecu`, `adapter`, `phone_receive`, `simulation` or `unknown`. The Bluetooth MVP uses `phone_receive` unless the adapter explicitly provides a measurement timestamp.
- `monotonic_ms` is elapsed time from the session's monotonic origin and is the primary ordering key within a session.
- `quality.status` is one of `valid`, `invalid`, `missing`, `stale`, `out_of_range`, `communication_error` or `unsupported`. Invalid readings remain stored but are excluded downstream.
- `interpolated` is always `false` at this input boundary. Interpolation is a declared pre-processing operation and creates a derived record later.
- A duplicate `(session_id, signal_id, monotonic_ms, ecu_id)` is deduplicated only when its value and provenance match; otherwise both records remain with a conflict flag.
- A unit conversion must be recorded in provenance. Silent conversion is prohibited.

## 4. DTC observation schema

```json
{
  "code": "P0301",
  "ecu_id": "ECM",
  "status": "confirmed",
  "observation_type": "snapshot",
  "observed_at": "2026-09-17T10:00:00.250Z",
  "monotonic_ms": 1250,
  "first_seen_at": null,
  "last_seen_at": null,
  "occurrence_count": null,
  "freeze_frame_ref": "FF-0001",
  "raw_status_byte": null,
  "provenance": {
    "service": "03",
    "response_reference": "raw/SES-20260917-0001/00000002.json"
  }
}
```

Rules:

- `code` is preserved exactly as received and normalized to uppercase for matching.
- `status` is one of `confirmed`, `pending`, `permanent`, `history`, `cleared`, `unknown`.
- `observation_type` is `snapshot`, `added`, `cleared`, `updated` or `freeze_frame`.
- A `snapshot` must declare whether it is complete in the session-level DTC state. An empty incremental response must not clear known codes.
- Freeze-frame data is referenced, not embedded into every DTC object. The referenced record contains ordinary sensor readings with their own timestamps and provenance.
- A DTC is evidence of an ECU-detected condition, not a root-cause decision.

## 5. Capability snapshot

```json
{
  "supported_signals": ["engine_rpm", "vehicle_speed", "coolant_temperature"],
  "supported_dtc_services": ["03", "07", "0A"],
  "ecu_ids": ["ECM"],
  "requested_rate_hz": {
    "engine_rpm": 1.0,
    "coolant_temperature": 0.2
  },
  "observed_rate_hz": {
    "engine_rpm": 0.98,
    "coolant_temperature": 0.20
  },
  "capability_source": "vehicle_query",
  "observed_at": "2026-09-17T10:00:00.000Z"
}
```

Capabilities describe what was supported or measured during this session. They do not claim that a signal exists on every vehicle of the same family. The input module should emit a capability snapshot at session start and update it if communication discovers a difference.

## 6. Starter signal profile

The following canonical identifiers are the proposed initial catalogue. The list is not a promise that every vehicle supports them. The adapter probes support and marks unavailable signals explicitly.

| Group | Canonical signals | Typical standard source where applicable |
|---|---|---|
| Minimum capture profile | `engine_rpm`, `vehicle_speed`, `coolant_temperature`, `calculated_engine_load`, `throttle_position` | Required policy profile; capability probing decides whether the session meets it |
| Thermal/ambient | `coolant_temperature`, `intake_air_temperature`, `ambient_air_temperature`, `barometric_pressure` | Standard identifiers where supported |
| Air/fuel | `intake_manifold_pressure`, `mass_air_flow`, `short_term_fuel_trim_b1`, `long_term_fuel_trim_b1`, `oxygen_sensor_voltage_b1s1`, `oxygen_sensor_voltage_b1s2`, `lambda_b1s1` | Availability and encoding vary; retain PID/DID and unit evidence |
| Electrical | `control_module_voltage` | Standard identifier where supported |
| State/history | `fuel_system_status`, `engine_runtime`, `distance_since_mil`, `time_since_cleared`, `monitor_readiness` | Standard diagnostic services/identifiers |
| Identity/capability | `vin`, `ecu_identity`, `calibration_id`, `supported_pids` | Vehicle-information/capability requests; not continuous telemetry; identity remains optional |

For the first test, the adapter targets a **Tata Nexon Creative Plus, 1.2L turbo petrol** over Bluetooth. The exact model year, engine code, market and adapter chipset must be recorded from the physical vehicle before vehicle-specific thresholds are published. The minimum capture profile is a product acceptance policy, not a claim that every BS4-and-later vehicle exposes every field. A missing minimum signal produces limited coverage and an explicit reason; it is never replaced by zero.

The standard references for the decoder are SAE J1979 and its Digital Annex for diagnostic services/data identifiers, and SAE J2012 for DTC definitions. They are references for interpretation, not a substitute for vehicle-specific documentation.

## 7. Input-module processing rules

The Phase 2 module must:

1. Validate the message against the schema and reject malformed messages with a machine-readable reason.
2. Persist the raw record before producing a normalized result.
3. Normalize identifiers and units while preserving the original value/response reference.
4. Order by monotonic time, detect sequence gaps and mark out-of-order records.
5. Deduplicate exact repeats and preserve conflicting duplicates.
6. Track per-signal observed rate, missingness, staleness and communication errors.
7. Keep DTC snapshots, deltas and clears semantically distinct.
8. Never interpolate at the input boundary.
9. Produce an explicit `unsupported` or `insufficient_data` state rather than a zero value.
10. Produce a normalized output record that downstream modules can replay deterministically.

## 8. What is frozen and what is still open

### Freeze now

- Versioned envelope and two-payload structure.
- Canonical signal-ID approach.
- UTC wall-clock plus monotonic session time.
- Raw provenance requirement.
- Explicit quality states.
- DTC complete-snapshot versus incremental semantics.
- Simulator, replay and vehicle as source types.
- No interpolation before the input module output.

### Decide before implementing the first adapter

- Exact first test vehicle: model, model year, engine code, fuel, market and transmission.
- Transport: which dongle/protocol and whether the first adapter is Bluetooth, USB or file replay.
- Minimum required starter signals for accepting an inspection.
- Categorical values remain deferred from sensor readings v1; DTC status and capability fields retain their explicit enumerations.
- Raw and normalized records are retained in encrypted phone-local storage. Raw capture retention is 90 days; normalized/derived evidence and certificate snapshots are retained for 12 months unless deleted earlier.
- Phone monotonic elapsed time orders the Bluetooth capture; phone UTC receive time is retained; ECU measurement time is nullable.
- VIN is attempted as an identity query but remains optional and may be unknown.
- Enhanced manufacturer-specific PIDs are excluded from v1.

These decisions affect the adapter and acceptance policy, but they do not require changing the downstream message shape.

## 9. Phase 2 acceptance test

Phase 2 is complete only when all of the following pass:

- A simulator creates a new scenario through configuration only.
- The simulator and a replay file emit the same `obd-input.v1` contract.
- A duplicate reading is handled deterministically.
- A conflicting duplicate is preserved and flagged.
- Out-of-order and missing samples are visible in quality metadata.
- A partial empty DTC update does not clear an existing DTC.
- A complete empty DTC snapshot clears only the session's known DTC set according to its declared semantics.
- Unsupported signals are reported as unsupported rather than zero.
- Raw and normalized records can be linked and replayed.
- A future vehicle adapter can emit the same message without changing Vehicle Context or later modules.
