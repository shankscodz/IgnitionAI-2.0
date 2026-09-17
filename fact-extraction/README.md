# fact-extraction

**Phase 1, Module 2** — Schema-constrained candidate fact extraction from source passages.

## Purpose

Turn source passages into candidate structured facts without granting them automatic authority. All output facts are `CANDIDATE` status and must pass through `consistency-checks` before being eligible for registry publication.

## Output Contract

See [`contracts/fact-contract.json`](contracts/fact-contract.json).

| Type | Description |
|---|---|
| `CandidateFact` | A single structured fact with provenance, type-specific fields, and extraction metadata |
| `ExtractionResult` | Per-passage outcome with all candidate facts |

## Key Behaviours

| Behaviour | Implementation |
|---|---|
| Absent info stays absent | Null fields are never inferred or plausibly completed |
| Threshold requires unit | `CandidateFact` constructor throws if unit missing on a THRESHOLD fact |
| Competing candidates preserved | Multiple facts from different sources with conflicting values are kept — not merged |
| Extraction confidence ≠ source authority | Separate fields; low-confidence extraction from an authoritative source stays low-confidence |
| Reproducibility | `extractorVersion` + `extractionSettings` stored on every fact |
| Multiple facts per passage | A single passage may yield facts of multiple types |

## Fact Types

| Type | When used |
|---|---|
| `THRESHOLD` | Numeric limits/ranges — always require `unit`, `thresholdLower`/`thresholdUpper` |
| `SYMPTOM_FAULT` | Symptom → possible fault relationships |
| `ACTION` | Diagnostic or repair actions |
| `DIAGNOSTIC_PRECONDITION` | Conditions required before an action is valid |
| `EXPECTED_OBSERVATION` | Expected sensor reading under specified conditions |
| `VEHICLE_APPLICABILITY` | Configuration constraints or compatibility rules |
| `TOOL_REQUIREMENT` | Required equipment |

## Status Lifecycle

Facts produced here start as `CANDIDATE`. The full lifecycle is owned by `consistency-checks`.

## Running Tests

```bash
javac -d out fact-extraction/src/**/*.java fact-extraction/test/*.java
java -cp out com.ignitionai.factextraction.test.FactExtractionTest
```

## Explicit Exclusions

- Source storage (belongs to `technical-sources`)
- Consistency validation (belongs to `consistency-checks`)
- Registry publication (belongs to `linked-registries`)
- Runtime rule application (facts are never runtime rules at this stage)
