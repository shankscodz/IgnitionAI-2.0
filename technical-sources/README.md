# technical-sources

**Phase 1, Module 1** — Immutable source records and addressable passages.

## Purpose

Define what counts as a technical source and retain an immutable, identifiable copy or reference that extracted facts can point back to. Every fact produced by the `fact-extraction` module must cite a `sourceId` and `passageId` from this module so provenance can always be verified.

## Output Contract

See [`contracts/source-contract.json`](contracts/source-contract.json).

| Type | Description |
|---|---|
| `TechnicalSource` | Versioned source record with passages |
| `IngestionResult` | Outcome + stored record + human and machine reason |
| `Passage` | Addressable text excerpt with page/section anchor |

## Key Behaviours

| Behaviour | Implementation |
|---|---|
| Duplicate detection | SHA-256 hash index; same bytes → `DUPLICATE` result, existing record returned |
| Revision tracking | `predecessorSourceId` link; `getRevisionChain()` traverses the chain |
| Missing metadata | All optional fields are `null`; never inferred or assumed |
| Unreadable files | >10% non-printable bytes → `UNREADABLE` with rejection code |
| Passage extraction | Sections detected by heading pattern; falls back to char-bounded chunks |

## Ingestion Status Lifecycle

```
PENDING → INGESTED    (successful)
PENDING → DUPLICATE   (same hash exists)
PENDING → UNREADABLE  (binary/scan content)
INGESTED → SUPERSEDED (newer revision ingested)
```

## Running Tests

```bash
javac -d out technical-sources/src/**/*.java technical-sources/test/*.java
java -cp out com.ignitionai.technicalsources.test.TechnicalSourceTest
```

## Explicit Exclusions

- OCR / scan support (separately estimated extension)
- Inferred metadata (publisher, dates, vehicle scope are always explicit)
- Fact extraction logic (belongs to `fact-extraction` module)
- Registry publication (belongs to `linked-registries` module)
