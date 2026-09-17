# Phase 1 — Decision Log

Versioned record of key decisions made during Phase 1 implementation.
All AI-proposed values must be confirmed by Shashank before being treated as established engineering facts.

---

## D-001 — Initial vehicle scope

| Field | Value |
|---|---|
| **Decision** | Initial vehicle family |
| **Proposed** | Tata Nexon Creative Plus, 1.2L turbo petrol |
| **Status** | ✅ CONFIRMED at product-scope level; exact model year, engine code and market remain to be recorded from the vehicle |
| **Impact** | Governs VehicleScope in source ingestion and VehicleApplicabilityEntry in linked registries |
| **Date** | 2026-09-17 |

The earlier VW EA888 Gen3 proposal is superseded by the Tata Nexon target. VW/Audi documents remain useful only as research fixtures unless their applicability to Tata is explicitly established.

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

## D-009 — Initial vehicle for Phase 2

| Field | Value |
|---|---|
| **Decision** | First vehicle target |
| **Chosen** | Tata Nexon Creative Plus, 1.2L turbo petrol |
| **Status** | ✅ CONFIRMED at product-scope level; exact model year, engine code and market remain to be recorded from the vehicle |
| **Impact** | Phase 2 adapter fixtures, capability probing and vehicle applicability metadata |
| **Date** | 2026-09-17 |

---

## D-010 — Phase 2 first transport

| Field | Value |
|---|---|
| **Decision** | Initial vehicle connection |
| **Chosen** | Bluetooth OBD adapter |
| **Status** | ✅ CONFIRMED as transport; exact adapter chipset/model remains to be recorded |
| **Impact** | Bluetooth adapter implementation and phone-side capture |
| **Date** | 2026-09-17 |

---

## D-011 — Minimum capture profile

| Field | Value |
|---|---|
| **Decision** | Minimum signals for an accepted Phase 2 inspection |
| **Chosen** | Engine RPM, vehicle speed, coolant temperature, calculated engine load and throttle position, subject to actual vehicle capability probing. A current-DTC scan must also be attempted. |
| **Status** | ✅ POLICY FROZEN; vehicle support remains an empirical capability result |
| **Rationale** | Conservative common baseline for an OBD inspection; no claim is made that every BS4+ vehicle exposes every signal. Missing signals produce limited coverage, not invented values. |
| **Impact** | Session acceptance and certificate coverage status |
| **Date** | 2026-09-17 |

---

## D-012 — Phase 2 data retention

| Field | Value |
|---|---|
| **Decision** | Initial retention location and policy |
| **Chosen** | Phone-local encrypted storage. Raw capture: 90 days. Normalized/derived evidence and certificate snapshots: 12 months, unless the user deletes them earlier. |
| **Status** | ✅ MVP POLICY FROZEN; cloud migration will introduce a new storage policy/version |
| **Impact** | Capture storage, export, deletion and privacy behaviour |
| **Date** | 2026-09-17 |

---

## D-013 — Time policy for Bluetooth capture

| Field | Value |
|---|---|
| **Decision** | Measurement time when ECU timestamp is unavailable |
| **Chosen** | Phone monotonic elapsed time is the ordering clock; phone UTC receive time is retained; ECU measurement time is nullable and never fabricated. |
| **Status** | ✅ CONFIRMED |
| **Impact** | Sampling, alignment, latency and episode calculations |
| **Date** | 2026-09-17 |

---

## D-014 — Enhanced manufacturer-specific PIDs

| Field | Value |
|---|---|
| **Decision** | Include enhanced/manufacturer-specific PIDs in Phase 2 v1? |
|---|---|
| **Chosen** | No. Phase 2 v1 uses the generic OBD contract and supported standard identifiers only. |
| **Status** | ✅ CONFIRMED |
| **Impact** | Adapter scope and signal catalogue; enhanced signals may be added in a versioned extension later |
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
