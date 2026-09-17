# linked-registries

**Phase 1, Module 4** — Four linked, versioned knowledge registries.

## Purpose

Publish checked and reviewed knowledge in four linked sub-registry components. This is the terminal module of Phase 1 — entries only reach here after passing `consistency-checks` and human review (REVIEWED status).

## Four Sub-Registries (Separate Components)

| Sub-Registry | Directory | Content |
|---|---|---|
| Vehicle Applicability | `src/vehicle-applicability/` | Configuration constraints, vehicle IDs, compatibility rules |
| Fault Knowledge Model | `src/fault-knowledge-model/` | Fault/symptom/observation relationships and conditions |
| Action Registry | `src/action-registry/` | Actions, preconditions, tools, outcomes, time estimates |
| Observation Registry | `src/observation-registry/` | Measured/computed quantities, units, definitions, limits |

## Output Contract

See [`contracts/registry-contract.json`](contracts/registry-contract.json).

| Type | Description |
|---|---|
| `RegistryRelease` | Immutable snapshot of all four registries at a point in time |
| `VehicleApplicabilityEntry` | Compatible vehicle configurations |
| `FaultKnowledgeEntry` | Fault/symptom relationships with cross-registry observation references |
| `ActionEntry` | Diagnostic/repair actions with sourced time estimates |
| `ObservationEntry` | Signal definitions — the authoritative source for signal metadata |

## Key Invariants

> **An incompatible vehicle MUST NOT receive any registry entry.**

- All analytical sessions **must pin to a specific `releaseId`**
- Cross-registry references are validated at entry creation time
- Draft entries are kept separate from published releases
- Released snapshots are immutable — knowledge edits create a new release
- Sourced time estimates are never invented — only populated when source text supports them

## Release Lifecycle

```
Draft store (CHECKED facts)
     │
     ├─► createDraftRelease()    → DRAFT release (for testing)
     └─► createPublishedRelease() → PUBLISHED release (for production)
```

## Running Tests

```bash
javac -d out linked-registries/src/**/*.java linked-registries/test/*.java
java -cp out com.ignitionai.linkedregistries.test.LinkedRegistriesTest
```

## Phase 1 Exit Demonstration

The full traceability chain is tested in `test_fullTraceabilityChain()`:

```
Source passage (SRC-00001#P001)
     │
     ├─► Candidate Fact (FACT-001)
     ├─► Check Result (PASS → CHECKED)
     ├─► Human Review (REVIEWED)
     ├─► Published Registry Entry (OBS-COOLANT-TEMP-FINAL in REL-YYYYMMDD-001)
     └─► Retrieval for matching vehicle VW/EA888 Gen3/petrol → 1 entry
         Retrieval for incompatible vehicle BMW/N20/petrol → 0 entries ✓
```

## Explicit Exclusions

- Fact extraction (belongs to `fact-extraction`)
- Consistency checks (belongs to `consistency-checks`)
- Human review workflow (external to this module; the module enforces the REVIEWED requirement at design level)
- OEM pipeline modules (Probabilistic Fault Graph, Value-of-Information Planner, etc. — out of Phase 1 scope)
