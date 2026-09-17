# IgnitionAI 2.0 — Pre-Owned Vehicle Diagnostic Pipeline

A modular knowledge engineering and vehicle health assessment system. Each directory maps 1-to-1 to a named box in the architecture diagram.

## Repository Structure

```
IgnitionAI 2.0/
├── technical-sources/        Phase 1 – immutable source records and addressable passages
├── fact-extraction/          Phase 1 – schema-constrained candidate fact extraction
├── consistency-checks/       Phase 1 – structural validation and contradiction detection
├── linked-registries/        Phase 1 – four linked, versioned knowledge registries
│   └── sub-registries:
│       ├── vehicle-applicability/
│       ├── fault-knowledge-model/
│       ├── action-registry/
│       └── observation-registry/
├── tools/
│   └── pipeline-runner/      Phase 1 CLI – exercises the full pipeline end-to-end
└── docs/
    └── phase1-decisions.md   Decision log for Phase 1
```

Phases 2–7 modules (obd-input-and-pre-processing, vehicle-context, direct-features, virtual-sensors, expected-behaviour-model, residual-and-uncertainty, anomaly-monitoring, episode-and-evidence-store, degradation-state, event-risk, indicator-severity, subsystem-health-scores, vehicle-health-index, certificate-snapshot, latex-certificate-generator, pre-owned-dealership-app) will be added in their respective phases.

## Phase 1 — Knowledge Engineering

See [`docs/phase1-decisions.md`](docs/phase1-decisions.md) for decisions and [`IgnitionAI_Seven_Phase_MVP_Action_Plan.md`](IgnitionAI_Seven_Phase_MVP_Action_Plan.md) for the full plan.

### Pipeline Flow (Phase 1)

```
TechnicalSource (document + passages)
        │
        ▼
CandidateFact  ◄── FactExtractor (schema-constrained, LLM-assisted)
        │
        ▼
ConsistencyCheckService  ──► quarantine store (contradictions)
        │
        ▼
LinkedRegistries  ──► RegistryRelease (pinned by releaseId)
        │
        ▼
RegistryQueryService  (lookup by vehicle context + observation ID)
```

### Running the Pipeline Runner

```bash
java -cp . tools.PipelineRunner --source-dir sample-sources/ --vehicle-family "VW_EA888_Gen3"
```

## Module Boundaries

Direct module-to-architecture-box mapping:

| Architecture Box   | Code Module             | Phase |
|--------------------|-------------------------|-------|
| Technical Sources  | `technical-sources/`    | 1     |
| Fact Extraction    | `fact-extraction/`      | 1     |
| Consistency Checks | `consistency-checks/`   | 1     |
| Linked Registries  | `linked-registries/`    | 1     |

Each module exposes an explicit input/output contract. No module merges responsibilities with another.
