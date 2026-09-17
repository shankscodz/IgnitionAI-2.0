# Phase 1 — Decision Log

Versioned record of key decisions made during Phase 1 implementation.
All AI-proposed values must be confirmed by Shashank before being treated as established engineering facts.

---

## D-001 — Initial vehicle scope

| Field | Value |
|---|---|
| **Decision** | Initial vehicle family |
| **Proposed** | VW EA888 Gen3 petrol (2013–2020) |
| **Status** | ⚠️ PENDING — Shashank to confirm |
| **Impact** | Governs VehicleScope in source ingestion and VehicleApplicabilityEntry in linked registries |
| **Date** | 2026-09-17 |

---

## D-002 — Initial source pack

| Field | Value |
|---|---|
| **Decision** | Which documents to ingest in Phase 1 |
| **Proposed** | 3–5 text-readable documents (service manuals, technical bulletins) |
| **Status** | ⚠️ PENDING — Shashank to provide documents and confirm redistribution rights |
| **Impact** | Source authority, vehicleScope, and extractable facts |
| **Constraint** | Text-readable documents only for MVP; scans/OCR are a separately estimated extension |
| **Date** | 2026-09-17 |

---

## D-003 — Document storage approach

| Field | Value |
|---|---|
| **Decision** | Where to store ingested source documents |
| **Chosen** | Local filesystem (MVP) — file path stored in `TechnicalSource.fileLocation` |
| **Rationale** | Lowest friction for MVP; the field is nullable and the interface is unchanged for cloud storage |
| **Date** | 2026-09-17 |

---

## D-004 — Fact extraction method

| Field | Value |
|---|---|
| **Decision** | How to extract structured facts from passages |
| **Chosen** | Rule-based schema-constrained extraction (pattern matching) for MVP |
| **Rationale** | Deterministic, reproducible, no external API dependency; LLM can be layered in later |
| **Constraint** | LLM may assist extraction, but absent information must remain absent — no plausible completion |
| **Date** | 2026-09-17 |

---

## D-005 — Conflict resolution policy

| Field | Value |
|---|---|
| **Decision** | How to handle contradictory threshold facts |
| **Chosen** | QUARANTINE — never average, never silently discard |
| **Rationale** | Averaging incompatible engineering values is technically incorrect and hides information |
| **Date** | 2026-09-17 |

---

## D-006 — Registry persistence (MVP)

| Field | Value |
|---|---|
| **Decision** | How to persist registry releases |
| **Chosen** | In-memory for MVP with RegistryRelease snapshots |
| **Proposed next step** | JSON serialisation of RegistryRelease to disk (simple and durable) |
| **Status** | ⚠️ PENDING — confirm before Phase 1 completion |
| **Date** | 2026-09-17 |

---

## D-007 — Implementation language

| Field | Value |
|---|---|
| **Decision** | Implementation language for Phase 1 modules |
| **Chosen** | Java (consistent with existing stack reference in action plan) |
| **Note** | If the existing project uses a specific Java version or build tool (Maven/Gradle), confirm before wiring modules together |
| **Status** | ⚠️ PENDING — confirm Java version and build tool |
| **Date** | 2026-09-17 |

---

## D-008 — Status of CHECKED facts

| Field | Value |
|---|---|
| **Decision** | Are CHECKED facts publishable to the registry? |
| **Chosen** | No — CHECKED means "passed automated checks". Human review (REVIEWED status) is required before publication |
| **Rationale** | Per the action plan: "Passing automated checks is not equivalent to technician review." |
| **Date** | 2026-09-17 |

---

## Template for new decisions

```markdown
## D-NNN — [Decision title]

| Field | Value |
|---|---|
| **Decision** | ... |
| **Proposed** | ... |
| **Chosen** | ... |
| **Status** | ⚠️ PENDING / ✅ CONFIRMED |
| **Impact** | ... |
| **Rationale** | ... |
| **Date** | YYYY-MM-DD |
```
