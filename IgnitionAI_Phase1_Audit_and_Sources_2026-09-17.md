# IgnitionAI 2.0 — Phase 1 implementation audit and source plan

Reviewed 17 September 2026 against the seven-phase action plan and the four knowledge-engineering modules in the architecture diagram.

Repository: `C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/`.

## Verdict

**Phase 1 has a useful implementation foundation, but is not complete or ready to publish trustworthy vehicle knowledge.** All four requested module directories exist. There are immutable data objects, ingestion/extraction/checking services, registry snapshots and tests. However, the submitted project does not compile as a whole, and its demonstration uses invented example documents rather than an acquired technical source pack.

The largest remaining work is preserving the meaning of technical statements, enforcing applicability and provenance, implementing durable storage and publication controls, and proving extraction on real documents. Adding more source files alone will not resolve these issues.

No original repository code was changed. For diagnosis only, I copied the implementation into an audit workspace and corrected the two observation-ID method calls there so the remaining registry tests and demonstration could run. Those scratch results are distinguished below from tests on the submitted code.

Your response “works” is treated as acceptance of VW EA888 Gen3 as an initial research scope. It does not identify an exact test vehicle. Model, year, engine code, market and calibration remain unresolved and must not be invented from that family name.

## 1. What has actually been implemented

| Diagram module | Working foundation present | Missing or unreliable | Assessment |
|---|---|---|---|
| Technical Sources | Source metadata, SHA-256 duplicate checks, passage IDs, revision links, in-memory lookup | PDF/HTML parsing, lossless section splitting, immutable original-file archive, durable storage, actual superseded-state update | Partial |
| Fact Extraction | Candidate schema, provenance fields, four regex extraction routines, extraction version/settings | Measurement identity is discarded; conditions and action semantics are lost; only threshold, symptom/fault and action categories are emitted; no real-document benchmark | Prototype |
| Consistency Checks | Required-field/unit checks, simple references, numeric plausibility, conflict decisions and queues | Quantity-aware comparison, unit conversion, complete reference resolution, correct numeric handling and symmetric conflict handling | Partial, with correctness defects |
| Linked Registries | Four entry types, draft collections, immutable release snapshots, release-specific queries | Compile error, durable releases, checked/reviewed publication gate, complete relationship validation, conservative unknown-context handling, conversion into all four registries | Partial |

Separate module directories match the requested architecture. They are not yet separate enforced build modules: no Maven/Gradle build definition is supplied in this folder. Plain `javac` was used for the audit. The README's run command assumes compiled classes already exist and refers to an absent `sample-sources` directory.

## 2. Verification performed

Environment: installed JDK 25; UTF-8 compilation; existing main-method test suites.

| Check | Result | Interpretation |
|---|---|---|
| Compile all original Java files | Failed: two compiler errors | Full submitted pipeline cannot run unchanged |
| Technical Sources tests, original source subset | 7 passed, 0 failed | Existing tests pass; coverage is limited |
| Fact Extraction tests, original source subset | 10 passed, 1 failed | Existing symptom/fault test fails |
| Consistency Checks tests, original source subset | 10 passed, 0 failed | Existing tests pass but miss important semantic errors |
| Linked Registries tests, scratch copy with only accessor mismatch corrected | 9 passed, 0 failed | Does not establish that the original project builds |
| Demo, same scratch copy | 2 synthetic sources; 10 candidates; 8 checked; 2 rejected; 0 quarantined | Produces only 1 applicability entry and 3 observations; no fault or action entries |

The failing existing extraction test is `test_symptomFaultExtraction`. Its example has a condition between “White smoke” and “may indicate”; the regex does not accommodate that phrasing in the smoke branch.

I also ran independent diagnostic probes. Results:

| Probe | Observed result | Required behaviour |
|---|---|---|
| Negative action: “Do not replace the oxygen sensor.” | Extracts an affirmative replacement action | Preserve prohibition or abstain |
| Temperature threshold with “at warm idle” | Numeric threshold survives; input/output identity and operating conditions are null | Retain quantity and applicability conditions |
| Coolant and intake-temperature ranges, same unit/context | Second range is quarantined as a conflict | Compare only facts about the same quantity and threshold role |
| Empty known-source set | A source-referencing fact passes | Reject unresolved source or explicitly return unresolved |
| NaN threshold bounds | Pass | Reject non-finite numbers |
| Temperature range −300 to −290 °F | Rejected as below absolute zero | Normalize temperature units before physical-bound checks |
| VW query with unknown engine, fuel and year | Matches a restricted EA888 petrol/year entry | Return indeterminate, not verified compatible |
| Published release from entry with no review evidence | Succeeds | Enforce publication eligibility in code |
| Heading-based source with a preamble and a section longer than 2,000 characters | Preamble and section tail disappear | Preserve all text with addressable chunks |
| Validate `{}` against each supplied JSON contract root | All four accept it | Provide an explicit root contract or use the intended definition via `$ref` |

Audit probe and output: [AuditProbe.java](C:/Users/SHASHANK/Documents/ChatGPT/IgnitionAI/work/phase1-audit-20260917/AuditProbe.java), [probe results](C:/Users/SHASHANK/Documents/ChatGPT/IgnitionAI/work/phase1-audit-20260917/probe-results.txt).

## 3. Findings, in repair order

### F1 — Build blocker: observation accessor mismatch

[RegistryQueryService.java:84](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/linked-registries/src/service/RegistryQueryService.java:84>) calls `getEntryId()` on `ObservationEntry`; that class exposes `getObservationId()`. Both duplicate checking and insertion fail compilation. Correct the accessor, then add one repeatable build/test command with nonzero exit status on failures.

### F2 — Extraction can reverse a prohibition and truncate a condition

[FactExtractor.java:48](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/fact-extraction/src/extractor/FactExtractor.java:48>) searches for action verbs anywhere in text, including inside a negated instruction. It also terminates action matches at `if`, `when` or `after`, without extracting the following condition.

Keep action polarity, preconditions, sequence constraints and source spans in the fact schema. Reject or flag ambiguous extraction. Neither an affirmative verb match nor a high extractor-confidence constant proves that a repair instruction is correct.

### F3 — Thresholds lose their measurement identity and role

[FactExtractor.java:116](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/fact-extraction/src/extractor/FactExtractor.java:116>) captures the quantity string, but does not put it into the fact. The same happens for ranges. Operating conditions are not populated. The schema has no dedicated quantity/observation identifier.

Add `quantity_id`, original quantity text, comparison operator, threshold role, condition expression, duration and source span. Separate normal operating range, ECU fault criterion, diagnostic test criterion and absolute physical limit. The runner currently transfers generic threshold bounds into `normalLimitLower/Upper`, which can misrepresent fault thresholds as normal-health baselines.

### F4 — Conflict detection is not comparing equivalent claims

[ThresholdConflictDetector.java:49](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/consistency-checks/src/checks/ThresholdConflictDetector.java:49>) uses applicability strings, condition strings and unit equality. It never matches the measured quantity. Equivalent units are not normalized. Different upper limits can also escape review because their half-infinite ranges overlap.

Compare canonical quantity + threshold role + overlapping structured vehicle scope + overlapping operating conditions + normalized units. Separate true contradiction from a stricter compatible constraint or unresolved scope. Quarantine the conflict relationship and block affected entries from publication pending resolution; do not simply favour whichever fact arrived first.

The existing test named `test_conflictQueueContainsBothSides` checks only that the second fact is queued. It does not verify its own stated invariant.

### F5 — Publication and applicability boundaries are not enforced

[RegistryQueryService.java:105](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/linked-registries/src/service/RegistryQueryService.java:105>) explicitly delegates review enforcement to the caller. No review record is required to create a published release. Add reviewer identity, time, decision, reviewed fact revision and unresolved-conflict checks at publication.

[VehicleApplicabilityEntry.java:80](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/linked-registries/src/vehicle-applicability/VehicleApplicabilityEntry.java:80>) treats missing target engine/fuel/year as if those restrictions need not be checked. Markets are stored but not evaluated. Use applicable / incompatible / insufficient-context outcomes. Add exact engine code, model, market and relevant calibration constraints where a source requires them.

The runner assigns every input document the same VW scope, even though `--vehicle-family` only changes a name. Replace this with per-source metadata. A Ford document must never inherit VW applicability because it was placed in the same directory.

### F6 — Real document ingestion and preservation remain incomplete

[SourceIngestionService.java:94](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/technical-sources/src/service/SourceIngestionService.java:94>) decodes raw bytes as UTF-8; it does not parse text-based PDFs. The runner only selects `.txt` and `.md`. PDF text extraction is distinct from OCR and is needed for the source pack below.

[SourceIngestionService.java:230](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/technical-sources/src/service/SourceIngestionService.java:230>) retains only the first 2,000 characters of each detected section; text before the first heading is skipped. Preserve all pages/sections and their page or table coordinates. Store original bytes in a content-addressed archive rather than retaining only the user's mutable file path.

### F7 — Registries and evidence are not durable or fully connected

Sources, candidate facts, checking queues and releases are all in memory. Comments mentioning optional JSON serialization are not an implemented persistence mechanism. Process-local counters also reset; restart-safe IDs and release storage are required.

The runner publishes observations from thresholds but does not transform extracted symptom/fault and action facts into their registries. Put this conversion in Linked Registries, with complete reference checks and provenance resolution. Keep the runner as orchestration tooling.

Some references are currently checked, but not all: source/passage IDs are largely format-checked; an empty source set bypasses existence checking; observation/action applicability references are not fully validated. Resolve the actual passage and verify that the cited span supports the extracted value.

### F8 — Contract and numerical validation need stronger checks

The JSON files define types under `definitions` but do not select a root type. That is usable as a schema library only if consumers explicitly reference a definition. No runtime JSON-schema validation integration was found. Add runnable contracts, conditional required fields and contract-to-Java fixtures.

Reject NaN/infinity. Normalize temperature units before checking physical limits. Distinguish absolute from gauge pressure rather than rejecting every negative pressure. Implement a unit registry with dimensions and conversions instead of only a whitelist.

### F9 — The demo source pack is invented and insufficiently labelled

[PipelineRunner.java:246](<C:/Users/SHASHANK/Downloads/IgnitionAI 2.0/tools/pipeline-runner/PipelineRunner.java:246>) creates sample text called a VW service manual and technical bulletin. These are code fixtures, not retrieved OEM publications. They include unsupported repair/threshold assertions. An empty or invalid source directory also falls back to the synthetic demo.

Keep fixtures explicitly synthetic, record that status in source metadata and prohibit their publication as OEM knowledge. An explicit real-source import failure should fail, not silently substitute fictional material. Source authority and extraction confidence need separate data fields; comments alone do not provide authority tracking.

## 4. Technical sources required

The following sources were located online during this audit. Links are references, not an ingested or licensed production knowledge pack. Public access must not be recorded as an open-source licence. OEM documents below carry their own notices; track permitted storage, extraction and redistribution separately before commercial ingestion.

### A. Initial VW/Audi source pack

| Priority / source | What to extract | Destination | Applicability and access |
|---|---|---|---|
| A1 — [Audi eSelf-Study Program 920243: third-generation 1.8L/2.0L EA888 engines](https://static.nhtsa.gov/odi/tsbs/2014/MC-10122162-9999.pdf) | Component names, system relationships, engine-variant differences, sensor/actuator roles | Vehicle Applicability; Fault Knowledge Model; Observation Registry | 72-page OEM training document, publicly hosted by NHTSA. It explicitly distinguishes itself from repair literature; use for architecture, not universal repair thresholds. Inspect pp. 5–9, thermal/lubrication sections and engine-management sections. |
| A2 — [VW Engine, Misfire Diagnostic Aid, transaction 2033805/8, 15 June 2023](https://static.nhtsa.gov/odi/tsbs/2023/MC-10238297-0001.pdf) | DTC/symptom relationships, diagnostic prerequisites, conditional actions, expected observations and repair-verification conditions | All four registries | Five-page OEM bulletin on NHTSA. Preserve its exact applicability table and revision metadata; do not assume applicability from engine-family name alone. Start extraction evaluation here. |
| A3 — [Earlier misfire bulletin, transaction 2033805/4, 28 July 2016](https://static.nhtsa.gov/odi/tsbs/2016/MC-10107504-9430.pdf) | Revision differences and supersession relationships | Technical Sources and consistency/revision tests | Historical version covering the stated 2008–2017 gasoline scope excluding Routan. Keep as a revision fixture; do not make it compete with a newer applicable revision as though both were current. |
| A4 — [2014 Volkswagen Passat Quick Reference Specification Book](https://static.nhtsa.gov/odi/tsbs/2014/MC-10122275-9999.pdf) | DTC identities, malfunction criteria, threshold operators and condition expressions | Observation Registry and Fault Knowledge Model | 95-page OEM document. For the initial petrol example inspect CPKA/CPRA section, printed pp. 1–19. Other engine sections must retain separate scope. These are ECU malfunction criteria, not ready-made VHI normal limits. |
| A5 — [Volkswagen erWin repair and maintenance information](https://erwin.vwgroup-datahub.com/category/repair-and-maintenance/0ZGSc0000000GxdOAE) | Exact vehicle repair procedures, prerequisites, tooling, wiring/test context and current specifications | Action Registry; Observation Registry; Vehicle Applicability | Official acquisition route. Portal information verified; subscription-protected vehicle documents were not accessed. Obtain documents matching the actual test vehicle and applicable access terms. |

**Initial evaluation pack:** A1, A2 and the relevant A4 section, with A3 as a revision test. Add a small selection of exact-vehicle A5 material before claiming executable repair coverage. These sources complement one another; they do not all apply to the same vehicle automatically.

Do not replace this pack with a folder of unscoped internet DTC descriptions. A DTC label is not a confirmed root cause, an ECU threshold is not a general health-score limit, and a training diagram is not a service procedure.

### B. Standards and identifiers

| Source | Why it is needed | Module/use | Access status |
|---|---|---|---|
| [SAE J1979 — E/E Diagnostic Test Modes](https://saemobilus.sae.org/standards/j1979_202505-e-e-diagnostic-test-modes) | Defines the emissions-related diagnostic service framework | Observation definitions now; OBD contract in Phase 2 | Official scope page verified; full standard is access-controlled |
| [SAE J1979 Digital Annex](https://saemobilus.sae.org/standards/j1979da_202510-j1979-da-digital-annex-e-e-diagnostic-test-modes) | Identifier definitions supporting OBD data interpretation | Observation Registry and acquisition decoder | Official catalogue page verified; it lists a newer July 2026 revision. Acquire and pin the exact applicable edition rather than silently mixing revisions |
| [SAE J2012 — Diagnostic Trouble Code Definitions](https://saemobilus.sae.org/standards/j2012_202509-diagnostic-trouble-code-definitions) | Standard DTC definitions and manufacturer-specific allocation boundaries | Fault Knowledge Model and DTC normalization | Official catalogue scope verified; obtain the corresponding digital annex/definitions under appropriate access |
| [NHTSA vPIC](https://vpic.nhtsa.dot.gov/) and [official API documentation](https://vpic.nhtsa.dot.gov/api/) | Manufacturer/VIN-derived identity metadata | Vehicle Applicability and later Vehicle Context | Public API. NHTSA states coverage is oriented to vehicles intended for US sale/import; expect limited data for other markets. It does not establish ECU calibration or every installed component |
| [NHTSA datasets and APIs — manufacturer communications](https://www.nhtsa.gov/nhtsa-datasets-and-apis) | Discovery index for additional OEM bulletins and their metadata | Technical Sources | Public discovery route; select documents by exact model/year and inspect their internal scope/revisions |

The standards supply interpretation and identifiers. They do not supply calibrated probabilities for your fault graph or enough data to train your degradation model.

### C. Supporting engineering references, kept separately scoped

| Source | What it supports | Constraint |
|---|---|---|
| [Ford/Lincoln MY 2024–2025 gasoline OBD operation summary](https://www.fordservicecontent.com/Ford_Content/catalog/motorcraft/OBD_Operation_Summary_to_Gasoline_MY_2024_2025.pdf) | Rich worked OEM monitor descriptions, conditions and diagnostic concepts; useful to test document/table extraction | 168-page Ford document. Use a separate Ford scope or research-only classification; do not apply its values to VW |
| [NIST Technical Note 1297](https://www.nist.gov/pml/nist-technical-note-1297) | Measurement uncertainty definitions and methodology | Method reference for Residual and Uncertainty; not vehicle-specific limits |
| [NIST EWMA control charts](https://www.itl.nist.gov/div898/handbook/pmc/section3/pmc324.htm) | EWMA algorithm and assumptions | Phase 4 method reference; automotive sampling/context still requires validation |
| [NIST CUSUM control charts](https://www.itl.nist.gov/div898/handbook/pmc/section3/pmc323.htm) | CUSUM algorithm and detection trade-offs | Phase 4 method reference; does not establish your vehicle detector settings |

Keep methodological documents in Technical Sources with purpose tags. They do not automatically produce vehicle repair actions or apply to a particular engine.

## 5. How to turn these sources into the required knowledge

### Step 1 — Register sources accurately

Create a source manifest with publisher, document identifier, revision, original URL, retrieved time, content hash, document purpose, authority, exact applicability, permitted use, original-file reference and supersession links. Add `synthetic=false` for acquired material and `synthetic=true` for fixtures. Unknown values remain explicit.

Document-wide scope is only the outer bound. A table or subsection may apply to a narrower engine, model year, market or calibration. Preserve that narrower scope on each fact.

### Step 2 — Extract a small benchmark by hand before scaling automation

Proposed starter benchmark: 40–60 facts across configuration, observations, threshold expressions, symptom/fault relations, conditional actions and tools. This is a target for evaluation, not a count already obtained.

Include negative instructions, multi-step conditions, table rows, two measurements sharing a unit, multiple engines in one document and superseded facts. Preserve page/section/table coordinates and a short supporting span. Evaluate held-out passages rather than adjusting patterns against every test example.

### Step 3 — Improve the fact representation

At minimum add:

```text
fact_id, fact_type, source_id, source_revision, source_span
quantity_id / component_id / dtc_id as appropriate
applicability: make, model, engine_code, fuel, market, years, calibration
conditions: structured predicates, regime, duration, prerequisites
threshold_role, operator, lower/upper/value, unit, pressure_reference
action_polarity, required_tools, expected_observations
extraction_method/version, extraction_confidence, source_authority
review_status, review_record, conflict_ids
```

Store source statements and your inferred engineering relationships separately. Do not invent Bayesian probabilities or expected readings from prose that does not contain them.

### Step 4 — Map verified facts to the four registries

- **Vehicle Applicability:** exact source-supported scope and exclusions.
- **Fault Knowledge Model:** symptom/DTC/observation relationships, with conditions and evidence strength.
- **Action Registry:** candidate tests/actions, prerequisites, tools, prohibited actions and observable outcomes. This provides primitives for later algorithmic planning; it need not be a fixed DTC-to-checklist database.
- **Observation Registry:** quantity IDs, units, definitions and contextual thresholds, with clear separation of normal limits from malfunction/test criteria.

### Step 5 — Publish and demonstrate the exit criterion

Demonstrate source → page/table span → candidate → check decision → review decision → registry entry → immutable persisted release → compatible-vehicle query. Restart the process and retrieve the same release. Repeat with incompatible and insufficient-context vehicles and show that neither gets unverified applicable advice.

## 6. Completion sequence

1. Fix compilation and establish a reproducible module build/test command.
2. Repair quantity identity, negation, conditions and threshold-role handling before adding more extraction volume.
3. Repair lossless passage splitting and introduce page-preserving PDF extraction.
4. Enforce structured applicability, actual reference resolution, units and publication eligibility.
5. Persist original sources, facts, check/review records and registry releases with restart-safe IDs.
6. Populate all four registries from the small benchmark pack and run the complete exit demonstration.

The current architecture can be kept. These changes belong inside its existing four modules; they do not require a new architecture or a restart of the project.

## 7. Limits of this review

This review inspected the supplied Java implementation, contracts, documentation and tests; compiled the project; ran the compilable original test suites; ran the remaining tests/demo in a minimally corrected scratch copy; and exercised targeted failure cases. It did not measure full real-document extraction accuracy, access paid OEM content, run a real vehicle or validate downstream diagnostic/health models.

Source availability and public catalogue contents were checked on 17 September 2026. The listed OEM publications are source candidates with explicit scope, not an assertion that their contents are current for every EA888 Gen3 vehicle. No commercial reuse rights or technician review have been assumed.
