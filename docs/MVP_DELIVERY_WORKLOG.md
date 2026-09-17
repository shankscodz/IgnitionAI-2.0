# Native MVP delivery

User goal: finish the complete app, address its shortcomings, push to GitHub, and deliver native Java Windows and Android apps plus a native Java simulator GUI. The attached audit requirements remain the acceptance checklist. OEM diagnostic planning and the pre-owned assessment pipeline both remain in scope.

## Baseline verified 2026-09-18

- Release branch is `master`, commit `11b399c`; only this branch remains remotely.
- The current suite passes, but the cumulative demo forces anomaly state, substitutes a healthy score, and exports after certificate validation fails.
- The dealership application is console-only. No Android or Windows graphical app exists in this repository.
- Phase 5 emits normalized degradation scores, whereas Phase 6 assumes percent scores. VHI inserts invented odometer and identity values.
- Application packaging, UI workflows, real cross-platform import/export, hardware capture verification, and customer validation are unfinished.

## Delivery sequence and evidence gates

1. Replace the cumulative demo with a reusable, causal application service; retain source/quality/identity through every phase. No invented health, vehicle identity, mileage, or calibrated probabilities.
2. Native Swing Windows assessment application and configurable simulator GUI; asynchronous work, replay import/export, history and certificate workflow. Verify actual graphical controls and packaged launch.
3. Native Java Android Activities plus Bluetooth connection/session lifecycle, app-private storage, replay/import/export, shared core, simulator UI, and installable APK. Verify build and emulator workflows; report physical-device checks separately.
4. Durable evidence/snapshots, bounded retention, strict contracts, scoring coverage and scale fixes, deterministic certificates and actual PDF compilation.
5. OEM hypothesis and diagnostic-test planning engine with explicit evidence and unknown hypotheses; UI presents algorithmic decisions and test feedback. Do not fabricate validated repair knowledge.
6. Clean checkout build, all meaningful suites, UI/runtime verification, APK and Windows distribution, published GitHub source/artifacts, final requirement-by-requirement audit.

No claim of customer-deliverable completion until these gates are evidenced. Simulation validates software behavior, not real vehicle diagnosis or predictive calibration.
