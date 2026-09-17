# IgnitionAI: seven-phase MVP action plan

Prepared for Shashank, 17 September 2026. Target: a demonstrable pre-owned pipeline MVP by the 25th. This plan estimates effort in hours; it does not assign tasks to calendar dates.

## 1. What we are building

Build the knowledge engineering pipeline and the shared telemetry, context, analytical and predictive modules, followed by the weighted Vehicle Health Index, vehicle certificate and pre-owned dealership app.

The finished MVP must accept configurable simulated telemetry, process it through the real analytical pipeline, explain assessed and unassessed vehicle condition, and produce a traceable certificate. Its input boundary must also accept future real-vehicle acquisition without replacing downstream analytical modules.

**Each named box in the architecture diagram remains a separate code module.** Do not merge expected behaviour with residual calculation, merge the three scoring modules, or bury certificate generation inside the app. The four registries remain separate components inside the Linked Registries module, as drawn. Direct Features and Virtual Sensors remain parallel consumers of Vehicle Context.

Your spoken phase numbers overlapped. This plan uses your intended order:

1. Knowledge engineering.
2. OBD input and pre-processing, plus the configurable generator.
3. Vehicle context, direct features and virtual sensors.
4. Expected behaviour, residuals and uncertainty, anomaly monitoring, episodes and evidence.
5. Degradation state and event risk.
6. Indicator severity, subsystem scores and Vehicle Health Index.
7. Certificate snapshot, LaTeX certificate generator and pre-owned dealership app.

The request for this deliverable is Markdown only. Implementing the certificate generator is future work described here; no PDF is being created with this plan.

## 2. Exact module boundaries

Use the following names as the architecture-to-code map. Each directory is independently testable and exposes explicit input/output contracts. For the existing Java project, a separate Gradle subproject for each box is the proposed implementation. Contracts belong to their producing module; shared build plumbing is not an additional analytical module.

| Phase | Diagram box | Proposed code module |
|---|---|---|
| 1 | Technical Sources | `technical-sources` |
| 1 | Fact Extraction | `fact-extraction` |
| 1 | Consistency Checks | `consistency-checks` |
| 1 | Linked Registries | `linked-registries` |
| 2 | OBD Input and Pre-processing | `obd-input-and-pre-processing` |
| 3 | Vehicle Context | `vehicle-context` |
| 3 | Direct Features | `direct-features` |
| 3 | Virtual Sensors | `virtual-sensors` |
| 4 | Expected-Behaviour Model | `expected-behaviour-model` |
| 4 | Residual and Uncertainty | `residual-and-uncertainty` |
| 4 | Anomaly Monitoring | `anomaly-monitoring` |
| 4 | Episode and Evidence Store | `episode-and-evidence-store` |
| 5 | Degradation State | `degradation-state` |
| 5 | Event Risk | `event-risk` |
| 6 | Indicator Severity | `indicator-severity` |
| 6 | Subsystem Health Scores | `subsystem-health-scores` |
| 6 | Vehicle Health Index | `vehicle-health-index` |
| 7 | Certificate Snapshot | `certificate-snapshot` |
| 7 | LaTeX Certificate Generator | `latex-certificate-generator` |
| 7 | Pre-owned Dealership App | `pre-owned-dealership-app` |

The configurable generator is the explicit addition you requested. Keep it as development tooling under `tools/obd-generator`, feeding the OBD module's public contract. It must not become a second analytical pipeline.

Preserve these diagram modules for the later OEM pipeline: Probabilistic Fault Graph, Value-of-Information Planner, Test Execution and Feedback, Structured Repair Plan, LLM Explanation Layer and OEM After-sales App. They are not implemented by these seven phases. Do not silently move their responsibilities into another module to make the MVP appear complete.

The repository already has broader projects such as `diagnostics-core`, `capture-protocol`, `simulator` and the application projects. Reuse suitable implementation code, but migrate ownership into the diagram-aligned modules as each phase proceeds. Retain existing entry points through temporary delegation where necessary; do not maintain two competing implementations or redesign the whole repository first.

## 3. AI-assisted effort estimate

These are **focused engineering hours**, including your decisions, AI-assisted implementation, review, debugging and module integration. They are not just time spent waiting for code generation. They assume one technical owner, the existing Java stack, one dealership app surface, local storage, a limited initial vehicle family, and no live OEM integration.

| Phase | AI-assisted hours | Principal outcome |
|---|---:|---|
| 1 — Knowledge engineering | 14–20 | Traceable, checked knowledge in four linked registries |
| 2 — OBD input and configurable generator | 16–24 | Stable input contract and reproducible configurable streams |
| 3 — Context, direct features and virtual sensors | 18–28 | Vehicle context and an expandable sensor catalogue/runtime |
| 4 — Anomaly detection and evidence | 20–30 | Contextual residuals, detectors and replayable episodes |
| 5 — Degradation and event risk | 14–22 | Working predictive interfaces and a simulation-verified implementation |
| 6 — Weighted health scoring | 9–14 | Explainable, coverage-aware weighted VHI |
| 7 — Certificate and dealership app | 18–28 | Usable app and traceable certificate workflow |
| **Base total** | **109–166** | All seven phases |
| Integration/rework allowance, approximately 15% | **16–25** | Cross-phase fixes and deployment issues |
| **Planning total** | **125–191** | Budget to use for capacity planning |

AI assistance is already included in these estimates. Do not discount them a second time. Calendar time also depends on your availability, decisions on the schema, delivery of the generator PDFs, and access to usable technical sources. If fewer than 125 focused hours are available before the 25th, this full scope should not be represented as a reliable solo commitment.

The estimate covers a broad catalogue of candidate virtual sensors and a selected executable starter set. It does not cover implementing every conceivable sensor, acquiring a large labelled fleet dataset, validating failure probabilities, unrestricted internet document ingestion, multi-platform app launches or production deployment at scale.

## 4. Phase 1 — Knowledge engineering

**Estimate: 14–20 hours.** Keep all four modules separate.

### 4.1 Technical Sources — 3–4 hours

**Purpose:** define what counts as a source and retain an immutable, identifiable copy or reference that extracted facts can point back to.

Work:

- Define `TechnicalSource` schema before importing documents.
- Include source ID, title, publisher, document type, original URL or file location, publication/revision date, retrieval date, language, document hash, access/use restrictions, vehicle scope and ingestion status.
- Preserve the original document and its revision. Store page numbers or section anchors with extracted text so later facts can cite the exact supporting passage.
- Support a small, deliberate source pack initially: accessible service documents, technical bulletins and relevant engineering material. Do not assume all OEM manuals are publicly available or redistributable.
- Record source authority and applicability separately. A credible document for the wrong engine must not govern the current vehicle.
- Handle duplicates, revised documents, unreadable files and missing metadata explicitly.
- Start with text-readable documents. Treat scans/OCR as a separately estimated extension if the supplied material requires it.

**Output:** versioned source records plus addressable text passages.

**Completion evidence:** import a small source pack; demonstrate duplicate detection, revision handling and retrieval of the original passage for each selected fact.

### 4.2 Fact Extraction — 4–6 hours

**Purpose:** turn passages into candidate structured facts, without granting them automatic authority.

Work:

- Define `CandidateFact` with fact ID, type, source ID, supporting passage, location, vehicle applicability, conditions, inputs, outputs and extraction version.
- Cover vehicle applicability, symptom/fault relationships, diagnostic preconditions, actions, tool requirements, expected observations, units and thresholds.
- Represent a threshold with its unit, operating conditions and applicable vehicle scope. Do not store an isolated number such as `90` without meaning.
- Use schema-constrained extraction. An LLM may assist extraction, but absent information must remain absent; require source support rather than plausible completion.
- Keep extraction confidence separate from source authority and technical approval.
- Allow multiple candidates from a passage and competing candidates from different sources.
- Store a reproducible record of the extractor/model version and extraction settings.

**Output:** candidate facts for Consistency Checks, never direct runtime rules.

**Completion evidence:** manually check a small reference set against its source passages; measure missing facts and unsupported additions; reject malformed output.

### 4.3 Consistency Checks — 3–4 hours

**Purpose:** validate structure and detect contradictions before facts enter published registries.

Work:

- Check required fields, numeric types, units and permitted conversions.
- Check references to vehicles, observations, actions and faults.
- Detect incompatible thresholds that overlap in vehicle applicability and operating conditions. Different conditions may legitimately produce different thresholds.
- Flag physically implausible ranges and actions requiring unavailable measurements or tools.
- Separate errors, warnings and unresolved conflicts. Quarantine contradictions instead of averaging them.
- Return machine-readable rejection reasons and a short human explanation.
- Track `candidate`, `checked`, `reviewed` and `published` status separately. Passing automated checks is not equivalent to technician review.

**Output:** checked facts, rejected facts and a conflict/review queue.

**Completion evidence:** deliberately inject wrong units, missing references and contradictory facts; confirm each produces the intended decision and preserves provenance.

### 4.4 Linked Registries — 4–6 hours

**Purpose:** publish checked knowledge in the four linked components drawn in the diagram.

Work:

1. **Vehicle Applicability:** configuration constraints, identifiers and compatibility rules.
2. **Fault Knowledge Model:** supported fault/symptom/observation relationships and their conditions.
3. **Action Registry:** actions, preconditions, tools, observable outcomes and sourced time estimates where available.
4. **Observation Registry:** measured or computed quantities, definitions, units, conditions and supported limits.

Use stable IDs, cross-registry references and registry-release versions. Provide lookup by vehicle context and observation ID. Keep draft content separate from the published release used by a capture session. Pin every analytical session to one registry release so knowledge edits cannot silently rewrite an old report.

**Output:** a queryable, versioned knowledge release with evidence links.

**Phase exit demonstration:** source passage → candidate fact → check result → published registry entry → retrieval for a matching vehicle. Show that an incompatible vehicle does not receive the entry.

## 5. Phase 2 — OBD Input and Pre-processing, plus the configurable generator

**Estimate: 16–24 hours.** The generator and real acquisition must produce the same public input contract.

### 5.1 Decide the contract together — 5–7 hours

The following is a **proposal to discuss, not a locked schema**. Start with a versioned message envelope containing two internal payloads: sensor readings and DTC observations.

| Area | Proposed fields and decisions |
|---|---|
| Envelope | `schema_version`, `session_id`, `message_id`, `source_type`, `sequence`, `emitted_at` |
| Vehicle reference | Internal vehicle reference and identity observations; unknown identity remains explicit |
| Sensor reading | Stable `signal_id`, optional PID/DID, ECU reference, value, unit, measured timestamp, received timestamp, validity and provenance |
| DTC observation | Code, ECU, status, observed timestamp and optional freeze-frame reference |
| Capabilities | Supported signals, ECU availability, requested rates and achieved rates |
| Session metadata | Start/end, connection events, clock information and acquisition configuration |
| Source provenance | `simulation`, `replay` or `vehicle`; generator version/seed for simulation; adapter information for real acquisition |

Record whether a DTC list is a complete scan or an incremental update. An empty partial message must not clear known DTCs. Define deduplication, out-of-order handling, unit conversion and schema evolution before implementation.

**Signal groups to review together:**

- Core operating state: engine RPM, vehicle speed, calculated load, coolant temperature, intake-air temperature, throttle position and fuel-control status.
- Air and fuel: manifold pressure, mass airflow, short- and long-term fuel trims by supported bank, oxygen/lambda measurements and relevant pressure measurements where available.
- Supporting context: barometric pressure, ambient temperature, control-module voltage, engine runtime and supported distance/time counters.
- Diagnostic state: DTC status, readiness, available freeze frames and supported reset-history observations.
- Vehicle information: supported VIN, ECU and calibration identifiers.
- Enhanced signals: transmission, manufacturer-specific misfire information, fuel-system detail and other signals only when access and definitions are established.

These are catalogue candidates, not a claim that every vehicle exposes them. Freeze a required starter profile, optional extensions and unsupported-value semantics together. Do not require every candidate signal to accept a session.

**Decisions to record:** initial vehicle/fuel family; mandatory starter signals; optional signals; timestamp precision; per-signal rate policy; transport into the app; DTC semantics; raw-data retention; versioning strategy.

### 5.2 Implement OBD Input and Pre-processing — 4–6 hours

- Validate messages against the agreed schema and retain the raw record.
- Normalize units and identifiers; attach quality flags rather than silently repairing implausible values.
- Deduplicate, detect gaps and preserve original timing. Do not assume all signals arrive simultaneously or at one fixed rate.
- Implement a bounded alignment policy for downstream computations, with maximum sample age and explicit interpolation permissions per signal.
- Publish accepted observations and diagnostic state with rejection reasons for invalid inputs.
- Keep acquisition-specific decoding inside this module's adapters. A future dongle adapter must emit the same contract consumed by the rest of the system.
- Add raw-session replay so saved captures can be run through newer algorithms reproducibly.

**Output:** normalized telemetry, DTC state, capability information and quality metadata.

### 5.3 Build one configurable generator — 7–11 hours

Review the generator PDF and reference image when you provide them. Until then, freeze only the shared input boundary; generator-specific mechanics remain provisional.

Implement a scenario configuration that composes:

- Vehicle profile and available sensor set.
- Operating sequence: start-up, warm-up, idle, changing load and shutdown.
- Baseline relationships between signals, with declared approximations.
- Fault/degradation mechanisms with onset, amplitude, duration, ramp, recovery and interactions.
- Sensor errors: bias, noise, drift, stuck readings, missing data and disconnects.
- Communication behaviour: different signal rates, jitter, delay, reordering and gaps.
- DTC/readiness behaviour specified independently from the continuous readings.
- Random seed, capture length, simulation speed and optional multi-session history.

Use composable mechanisms and parameter ranges. Four or five example presets may demonstrate the system, but they must be configuration files using the same engine, not hard-coded scenario branches.

Keep two outputs separate: the public OBD stream and private simulation ground truth for evaluation. The analytical pipeline must not receive the injected fault label, true degradation state or planned failure time as an input feature.

**Phase exit demonstration:** create an unseen scenario by changing configuration only; reproduce it with its seed; feed its stream through the OBD module; replay it with the same accepted observations. Confirm that the same module can consume a captured-file fixture using the real-vehicle contract. Physical dongle compatibility still requires later hardware testing.

## 6. Phase 3 — Vehicle Context, Direct Features and Virtual Sensors

**Estimate: 18–28 hours.** Three distinct modules, with Direct Features and Virtual Sensors operating in parallel.

### 6.1 Vehicle Context — 5–7 hours

Create a versioned vehicle-context object with four explicit sections:

1. **Identity:** vehicle reference, available VIN/ECU/calibration identity, evidence source and confidence.
2. **Configuration:** engine/fuel/transmission family and other supported attributes, resolved against applicability knowledge. Unknown values remain unknown.
3. **Operating conditions:** current regime, temperatures, speed/load state and transition history.
4. **Evidence quality and history:** signal availability, measured rates, gaps, readiness/reset information, prior sessions and accumulated observed exposure.

Separate relatively stable configuration from rapidly changing state. Publish immutable context snapshots or versioned updates; do not pass a mutable global object that silently changes during analysis. Attach context version and time to every downstream result.

Prefer automated identity retrieval and authoritative lookup. If identity cannot be resolved, select a declared generic/unknown profile or abstain from vehicle-specific calculations. Do not invent an exact configuration from weak clues.

**Completion evidence:** handle known identity, unknown identity, missing configuration, changing operating regime and incomplete evidence without fabricating values.

### 6.2 Direct Features — 4–6 hours

Create a feature catalogue before coding the first calculations. Each entry specifies its ID, purpose, inputs, units, operating preconditions, formula/window, quality requirements and output definition.

Starter candidates include:

- Windowed mean, range, variability and slope for supported signals.
- RPM stability within a qualified idle regime.
- Coolant warm-up rate within a qualified warm-up interval.
- Fuel-trim summaries by bank and operating regime.
- Voltage minimum and variability within defined operating conditions.
- Time spent in each observed regime and missing-data fraction.

Keep signal-specific definitions explicit. A formula combining two values is not automatically physically valid across every vehicle. A statistic from an invalid or insufficient window must return `insufficient_data`, not zero.

**Output:** feature values with time support, units, quality, input references and definition version.

### 6.3 Virtual Sensors — 9–15 hours

**Purpose:** implement an expandable catalogue and a runtime that discovers sensor definitions without edits to the core program.

Research broadly and catalogue candidates across air path, combustion/fuelling, thermal behaviour, electrical supply, powertrain response and degradation proxies. Record both feasible candidates and those requiring unavailable signals. “All possible virtual sensors” is an open-ended research scope; the deliverable is a broad, expandable catalogue, not a false claim of mathematical completeness.

For each candidate record:

- Physical quantity or phenomenon estimated, and whether the output is an estimate or a proxy.
- Source reference, governing equation/model family and applicability.
- Inputs, units, sampling/time-alignment needs and operating preconditions.
- Outputs, units, uncertainty representation and quality status.
- Calibration/training needs, expected failure modes and validation method.
- Implementation status: researched, executable, simulation-checked or vehicle-validated.

**Plug-and-play contract:**

- Discover a manifest in each sensor folder at startup; installation followed by restart is sufficient for the MVP. Live hot reload is not required.
- Manifest fields include ID, version, input/output specifications, dependencies, vehicle applicability, operating preconditions, implementation reference, model/calibration reference and resource limits.
- Small formula-based sensors can be added as one declarative sensor file using supported operators.
- More complex sensors use a supported plugin interface plus a packaged implementation/model. An arbitrary new algorithm cannot run merely because its name appears in a configuration file.
- Validate definitions at discovery, detect duplicate IDs and dependency cycles, and check dimensional compatibility.
- Schedule only sensors whose requirements are satisfied. Return `unsupported`, `missing_inputs` or `not_applicable` with reasons otherwise.
- Isolate a failing plugin so it cannot terminate the capture session.
- Feed the rest of the pipeline through the same output contract, whether the sensor is formula-based or model-based.

For the hour estimate, implement approximately 3–5 feasible starter sensors selected after the OBD signal decision. Research candidates such as an intake-air estimate, thermal-response estimate or charging-behaviour proxy, but do not claim those are ready until their inputs, physical assumptions and reference methods are checked.

**Phase exit demonstration:** add a sensor definition without changing core source code, restart, discover it and obtain outputs. Remove a required input and observe an explicit unavailable result. Confirm the UI and downstream consumers enumerate its metadata rather than requiring a hard-coded sensor switch statement.

Discovery makes a sensor executable. It does not automatically make it suitable for health scoring: expected-behaviour, anomaly and scoring policies must also be configured and validated for its outputs.

## 7. Phase 4 — Complete anomaly detection process

**Estimate: 20–30 hours.** Four separate modules, exactly as in the diagram.

### 7.1 Expected-Behaviour Model — 6–9 hours

- Define a prediction contract for an indicator given vehicle context and operating regime.
- Return expected value or range, predictive uncertainty, applicability, training/calibration provenance and model version.
- Start with interpretable reference curves or simple fitted models where evidence supports them. Keep the interface open to later model replacements.
- Use declared reference data and simulated data for initial integration. Synthetic fit quality is not evidence of real-vehicle accuracy.
- Exclude abnormal calibration samples and prevent target leakage: a model should not simply use the current observed target to reproduce itself as the expected value.
- Detect unsupported context or operation outside the model's supported range; abstain or widen uncertainty according to policy.
- Keep baseline calibration distinct from the active inspection. Do not automatically learn an ongoing fault as normal.

**Output:** time-aligned expected observations with uncertainty and provenance.

### 7.2 Residual and Uncertainty — 4–6 hours

- Compute `residual = observed - expected` for the same quantity, unit and time support.
- Preserve signed residuals; produce magnitude only where a downstream rule requires it.
- Combine observation error, parameter/model uncertainty, mismatch and timing uncertainty under a documented approximation.
- Account for covariance where known. If prediction and measurement share inputs, do not blindly add all variances as though they were independent.
- Avoid counting input uncertainty twice when it is already included in a model's predictive variance.
- Compute a normalized residual using a valid residual scale, with a configured numerical floor. Record whether that scale is statistically estimated or an engineering tolerance.
- Return unavailable when uncertainty or alignment cannot support the computation. A normalized residual is not automatically a calibrated probability.

**Output:** signed residual, normalization scale, normalized residual, uncertainty breakdown and quality status.

### 7.3 Anomaly Monitoring — 5–7 hours

- Implement persistence, EWMA and CUSUM as selectable monitoring methods.
- Configure detector parameters by indicator and operating regime; do not apply one universal threshold to every sensor.
- Use elapsed time for persistence when sampling is irregular.
- Define warning/active/recovering/cleared states, hysteresis, cooldown and gap-handling rules.
- Define how state resets or pauses when vehicle context changes or the engine disconnects.
- Avoid treating three detectors responding to the same evidence as three independent fault confirmations.
- Record the detector version and parameter set with each transition.

**Output:** detector events with onset, confirmation, magnitude, supporting evidence and state changes.

### 7.4 Episode and Evidence Store — 5–8 hours

- Store capture sessions, timestamped observations, features, residuals, detector events and episode records with linked IDs.
- Define sampling interval, analysis window, anomaly episode and capture session separately.
- An episode has a candidate onset, confirmed detection, last abnormal observation, recovery/closure and duration. Preserve the distinction between event time and the later time at which it was confirmed.
- Use configurable persistence and recovery rules. Episode duration is an outcome of the signal, not a fixed timestamp length.
- Define how overlapping anomalies are linked and how gaps split or censor an episode.
- Retain immutable raw evidence and versioned derived results; rerunning a newer model creates another analysis result rather than overwriting history.
- Provide queries by session, vehicle, indicator and time range, plus replay/export for debugging.

**Phase exit demonstration:** normal run, isolated spike, persistent shift, gradual drift, operating-regime transition and missing-data interval. Demonstrate that a spike need not become an episode, persistent evidence does, and every episode links back to its observations and versions. Measure false alerts and detection delay on held-out generated scenarios without claiming real-world performance.

## 8. Phase 5 — Degradation State and Event Risk

**Estimate: 14–22 hours.** Two separate modules; both consume shared evidence, and Event Risk may also consume Degradation State.

### 8.1 Degradation State — 7–11 hours

- Select the indicators whose evolution can reasonably represent condition change under comparable operating contexts.
- Define a degradation-state contract: estimated level, direction, rate, uncertainty, observed exposure, required history and data-sufficiency status.
- Implement a simple state-space trend model that separates latent condition change from measurement noise. Keep the transition and observation assumptions configurable.
- Handle irregular sessions and elapsed time explicitly. Do not interpret an unobserved month as a month of measured use.
- Use context-conditioned indicators from earlier modules; do not duplicate expected-behaviour logic here.
- Mark discontinuities such as changed calibration or known repair when evidence exists. Do not silently connect incompatible sessions into one smooth trend.
- Generate longitudinal simulated histories with known stable, worsening, recovering and noisy conditions. Keep future observations out of past predictions.

**Output:** condition trend and uncertainty, or a precise insufficient-history explanation.

### 8.2 Event Risk — 7–11 hours

- Define one initial endpoint and horizon before selecting a model. A recurring analytical anomaly, confirmed fault and physical failure are different endpoints.
- Implement a discrete-time hazard/survival interface as the proposed MVP model family.
- Specify the time origin, exposure units, event labels, right-censoring and allowable predictors.
- Calculate horizon-specific risk from successive conditional hazards; check bounded probabilities and consistent survival behaviour.
- Use generated labelled histories to exercise training/inference and censoring handling. Label outputs as simulation-only until real outcome data support calibration.
- Prevent post-event information and future repairs from leaking into earlier predictions.
- Expose `available`, `insufficient_history`, `unsupported_endpoint` and `uncalibrated` states.

**Phase exit demonstration:** reconstruct an injected worsening trend; show uncertainty under sparse history; produce a reproducible simulated risk for a named endpoint/horizon; show unavailable risk for a real session without sufficient validated support.

**MVP boundary:** this phase implements the predictive workflow. A single short inspection does not establish long-term degradation, and working model code does not establish a valid probability of component failure. Dataset acquisition and real-world calibration remain separate work beyond this estimate.

## 9. Phase 6 — Weighted vehicle health scoring

**Estimate: 9–14 hours.** Preserve all three scoring modules.

### 9.1 Indicator Severity — 3–5 hours

- Define the mapping from each qualified normalized residual to severity between 0 and 1.
- Configure normal tolerance and severe-deviation limits per indicator, with one-sided or two-sided behaviour as appropriate.
- Include declared temporal effects such as persistence and recurrence over a defined assessment period. Use time-aware aggregation so a faster-sampled signal is not automatically penalized more.
- Keep the contributions inspectable: deviation, duration, recurrence and the resulting severity.
- Avoid repeated penalties for the same underlying observation under several detector names.
- Store mapping version, applicability, minimum evidence and invalid-data handling.

Proposed starting form, to be calibrated per indicator:

```text
instant_severity = clip((deviation - normal_limit)
                        / (severe_limit - normal_limit), 0, 1)
indicator_severity = configured_temporal_aggregation(instant_severity)
indicator_health = 100 * (1 - indicator_severity)
```

**Output:** indicator severity and health, or unassessed status with a reason.

### 9.2 Subsystem Health Scores — 3–4 hours

- Group indicators by an explicit subsystem definition.
- Store nonnegative internal weights in a versioned policy.
- Calculate a weighted mean over eligible indicators, reporting how much of the planned weight was assessed.
- Do not assign perfect health to a missing indicator. If minimum coverage is not reached, mark the subsystem unassessed.
- Avoid overweighting highly correlated indicators by accident; document weight choices.

```text
subsystem_health = sum(weight_i * indicator_health_i) / sum(weight_i)
                  for eligible assessed indicators i
subsystem_coverage = assessed_applicable_weight / planned_applicable_weight
```

**Output:** subsystem score, coverage, indicator contributions and important findings.

### 9.3 Vehicle Health Index — 3–5 hours

- Store nonnegative external subsystem weights separately from internal indicator weights.
- Aggregate eligible subsystem scores using the chosen weighted-sum approach.
- Define minimum overall coverage and any mandatory subsystem requirements for publishing a headline score.
- Keep critical findings visible regardless of the average. A high average must not erase an important issue.
- Keep current VHI separate from predicted risk; do not mix a trend forecast into today's score without an explicit future policy change.
- Report scope, coverage and policy version beside the number. Scores computed with materially different scope/policies are not automatically comparable.

```text
VHI = sum(external_weight_s * subsystem_health_s) / sum(external_weight_s)
      for eligible assessed subsystems s
```

If the publication rule is not satisfied, output `VHI unavailable` and show available subsystem findings. Do not manufacture an overall value merely to fill the certificate.

**Phase exit demonstration:** independently calculate a small example; match every intermediate result. Check that worsening an indicator cannot improve its score under unchanged context, missing evidence is not treated as healthy, and critical findings survive aggregation.

## 10. Phase 7 — Certificate and pre-owned dealership app

**Estimate: 18–28 hours.** Three separate modules; the app invokes the analytical modules and displays their outputs.

### 10.1 Certificate Snapshot — 4–6 hours

Create an immutable report object containing:

- Certificate ID, revision, inspection time and issuance time.
- Vehicle identity/configuration and their sources.
- Capture source, duration, achieved operating conditions and data quality.
- Supported, assessed and unassessed systems with reasons.
- VHI, subsystem scores, coverage, internal/external weights and scoring-policy version.
- DTCs, analytical findings, episodes and evidence references.
- Degradation and event-risk outputs, with endpoint, horizon, uncertainty and availability status.
- Recommendations and their producing-module/version references when available.
- Reference-test results and technician acknowledgement if present.
- Registry, feature, virtual-sensor, expected-model and detector versions.
- Status: `simulation`, `preliminary` or `qualified_within_stated_scope`, with a defined eligibility policy.

The snapshot is the single report source for the app and LaTeX generator. Issued reports must not change when a model is updated; revisions create new snapshots with a link to the previous one.

**Important dependency:** the diagram supplies algorithmically selected recommendations from Structured Repair Plan in the OEM branch. That module and its diagnosis dependencies are outside this seven-phase scope. Preserve the input contract and show `Recommendations unavailable — diagnostic planning not included in this MVP` when no valid plan exists. Evidence-backed findings and data-collection guidance may still be displayed as such; do not present them as generated repairs or let an LLM invent the missing plan. Full algorithmic repair recommendations require separately scoped work on those existing diagram modules.

### 10.2 LaTeX Certificate Generator — 5–8 hours

- Accept a validated snapshot, not live application state.
- Create sections for summary, subsystem assessment, predictive degradation, recommended actions and the evidence appendix.
- Print unsupported or unavailable results clearly while keeping every section present.
- Include relevant plots, timestamps, units, DTC status, evidence references and versions.
- Escape external text safely; compile with shell escape disabled, bounded execution and captured errors.
- Support long identifiers, multiple findings, page breaks and charts without overlapping content.
- Store the snapshot reference/hash alongside the generated artifact.
- Add a QR only if there is a real verification destination. Local-only issuance may use a visible certificate ID and integrity hash instead; a hash alone is not issuer authentication.

**Completion evidence:** render simulation, preliminary, missing-data and long-content examples; visually inspect them; confirm the displayed values match their snapshots exactly.

### 10.3 Pre-owned Dealership App — 9–14 hours

Use one existing application surface for the MVP; selecting desktop versus another surface is an early implementation decision. Avoid estimating several app platforms as though they were one.

Implement this workflow:

1. Start an inspection and choose simulation, replay or an available real acquisition adapter.
2. Select a scenario configuration for simulation; keep advanced generator controls separate from the normal inspection experience.
3. Show connection, identity resolution, capture progress, signal availability and operating-condition coverage.
4. Display live analytical status and meaningful data-quality problems.
5. Review VHI, subsystem scores, coverage, DTCs and the evidence supporting findings.
6. Show degradation/risk where supported, and specific reasons where unavailable.
7. Finalize a snapshot, generate the certificate and reopen previous inspections.

The app must not recompute health scores, thresholds or model decisions. It renders authoritative module outputs. Keep simulation status visible throughout; handle failed capture, interrupted sessions, unsupported sensors and certificate generation failure without losing evidence.

**Phase exit demonstration:** a user can generate a new scenario, capture it, inspect the evidence and scores, create a certificate, close the app, reopen the inspection and retrieve the same issued result.

## 11. Integration order and acceptance rules

Build in the seven-phase order, using contract fixtures at each boundary. Do not postpone integration until Phase 7. A thin command-line runner is enough to exercise the pipeline before the dealership app is ready; it is test tooling, not another architecture module.

For every module:

1. Write its input/output schema, responsibilities and explicit exclusions.
2. Add a minimal normal fixture and meaningful failure/insufficient-data fixtures.
3. Let AI implement against those contracts, within that module boundary.
4. Review physical assumptions, data handling and dependency direction yourself.
5. Run module checks and one integration check against the previous stage.
6. Record known limitations and demonstrated behaviour before proceeding.

Allow direct feature/virtual sensor dependencies only through declared contracts; reject cycles. Models receive knowledge through the linked registries and vehicle context where applicable. The evidence store provides the traceable basis for predictions, scores and certificates. The app owns workflow orchestration, not duplicated analytical rules.

Use stable IDs, explicit units, timestamps, availability states and version references throughout. Every computed value should answer: what produced it, from which observations, under which vehicle context, using which version?

## 12. What “MVP complete” means

The MVP is complete when all of the following are demonstrated:

- Every in-scope architecture box exists as a distinct module with a tested contract.
- Knowledge can be ingested, extracted, checked and queried with source provenance.
- One configurable generator produces varied, reproducible scenarios through the real input boundary.
- Context handling distinguishes known, inferred and unavailable information.
- A new compatible virtual sensor can be installed without editing the core program.
- Expected behaviour, residuals, uncertainty, monitoring and episodes run as separate traceable stages.
- Degradation and event-risk workflows execute on appropriate generated histories and abstain when support is insufficient.
- Weighted indicator/subsystem/VHI calculations are reproducible and expose coverage.
- The dealership app supports the inspection-to-certificate workflow.
- Certificates identify simulation/preliminary status, evidence limitations and unavailable recommendations rather than filling gaps with invented output.

This is an operational software MVP. Real-vehicle performance, reliable long-term failure forecasts and a complete OEM diagnostic/repair product require further data and validation.

## 13. Decisions and inputs needed during execution

| When | Decision/input | What can proceed before it is resolved |
|---|---|---|
| Phase 1 start | Initial vehicle scope and accessible source pack | Source/fact schemas and consistency framework |
| Phase 2 contract review | Exact signal list, units, timing and message semantics, decided with you | Draft schema and validation fixtures |
| Phase 2 generator work | Your generator PDF and reference image | Input boundary and generator manifest proposal |
| Phase 3 | Initial executable virtual sensors and calibration support | Catalogue research and plugin runtime |
| Phase 4 | Indicator baselines, uncertainty assumptions and detector policies | Module interfaces and synthetic verification cases |
| Phase 5 | Initial endpoint, horizon and required history | Predictive interfaces and simulated longitudinal data |
| Phase 6 | Internal/external weights, minimum coverage and critical findings | Scoring engine and hand-calculated examples |
| Phase 7 | App surface and certificate issuance/status policy | Snapshot contract and report sections |

Freeze decisions in small versioned configuration files and a short decision log. AI may propose values, but it must not silently choose vehicle applicability, fault thresholds, health weights or failure endpoints and present them as established engineering facts.

## 14. How to protect the target without changing the architecture

Keep all seven phases and all their module boundaries. Reduce initial breadth where necessary: one vehicle family, one acquisition contract, a small source pack, a few executable virtual sensors, one predictive endpoint, one scoring policy and one dealership app surface.

Maintain the broad virtual-sensor catalogue and extension mechanisms even when only a subset is executable. Keep every report section, with honest availability states. Defer fleet-scale training, production cloud operations, extensive document-format support, live plugin reload, extra app platforms and the OEM branch.

Before each phase, compare remaining focused hours with the estimate. After each phase, replace estimates with actual effort. This gives you a concrete way to manage the 25th target without letting AI-generated code volume substitute for demonstrated progress.
