# consistency-checks

**Phase 1, Module 3** — Structural validation and contradiction detection for candidate facts.

## Purpose

Validate structure and detect contradictions before facts enter published registries. This module is the gate between raw candidate facts and the linked registries.

## Output Contract

See [`contracts/check-contract.json`](contracts/check-contract.json).

| Type | Description |
|---|---|
| `CheckResult` | Per-fact check outcome with machine codes and human explanation |
| Checked store | Facts that passed all automated checks (CHECKED status) |
| Rejected store | Facts that failed — preserved with reasons, never silently discarded |
| Quarantine store | Conflicting facts — preserved for explicit resolution, never averaged |

## Four-Step Pipeline

```
CandidateFact
     │
     ├─► FieldValidator         (required fields, units, numeric types)
     ├─► ReferenceValidator     (sourceId existence, passageId format, no wildcards)
     ├─► PlausibilityChecker    (temperature, pressure, RPM physical ranges)
     └─► ThresholdConflictDetector  (non-overlapping ranges → QUARANTINE)
          │
          ▼
     CheckResult  ──► CHECKED / REJECTED / QUARANTINED
```

## Status Transitions

| Transition | Condition |
|---|---|
| CANDIDATE → CHECKED | All checks PASS or WARN |
| CANDIDATE → REJECTED | Any check returns REJECT |
| CANDIDATE → QUARANTINED | Conflict with existing threshold fact |

> **IMPORTANT:** CHECKED ≠ PUBLISHED. Human review (REVIEWED status) is a separate explicit step.

## Rejection Codes

| Code | Check | Meaning |
|---|---|---|
| `MISSING_REQUIRED_FIELD` | FieldValidator | Required field absent |
| `MISSING_UNIT` | FieldValidator | THRESHOLD fact has no unit |
| `UNRECOGNIZED_UNIT` | FieldValidator | Unit not in known registry |
| `INVALID_NUMERIC_VALUE` | FieldValidator | lower > upper, or similar |
| `MISSING_EXTRACTOR_VERSION` | FieldValidator | Reproducibility not guaranteed |
| `INVALID_SOURCE_REFERENCE` | ReferenceValidator | sourceId unknown or malformed |
| `INCONSISTENT_PASSAGE_REF` | ReferenceValidator | passageId doesn't match sourceId |
| `INVALID_VEHICLE_APPLICABILITY` | ReferenceValidator | Wildcard like "all vehicles" |
| `IMPLAUSIBLE_RANGE` | PlausibilityChecker | Below absolute zero, negative pressure, etc. |
| `THRESHOLD_CONFLICT` | ConflictDetector | Non-overlapping ranges for same scope/conditions |

## Running Tests

```bash
javac -d out consistency-checks/src/**/*.java consistency-checks/test/*.java
java -cp out com.ignitionai.consistencychecks.test.ConsistencyCheckTest
```

## Explicit Exclusions

- Technician review (belongs to the REVIEWED → PUBLISHED step in linked-registries)
- Registry publication (belongs to `linked-registries`)
- Fact extraction (belongs to `fact-extraction`)
