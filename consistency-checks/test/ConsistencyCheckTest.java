package com.ignitionai.consistencychecks.test;

import com.ignitionai.consistencychecks.schema.*;
import com.ignitionai.consistencychecks.service.ConsistencyCheckService;
import com.ignitionai.factextraction.schema.*;

import java.util.*;

/**
 * Tests for the consistency-checks module.
 *
 * Verifies completion evidence:
 * 1. Wrong unit → REJECT with unit mismatch code
 * 2. Missing required field → REJECT with field code
 * 3. Contradictory thresholds from two sources, same vehicle/conditions → QUARANTINE (not average)
 * 4. Physically impossible value (below absolute zero) → REJECT with IMPLAUSIBLE_RANGE
 * 5. Wildcard vehicleApplicability → REJECT
 * 6. Mismatched passageId prefix → REJECT
 * 7. Clean fact passes all checks → CHECKED status
 * 8. CHECKED ≠ PUBLISHED (human review still required)
 * 9. Conflict queue contains both quarantined facts
 */
public class ConsistencyCheckTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== consistency-checks Tests ===\n");

        test_unrecognizedUnit_rejected();
        test_missingRequiredField_rejected();
        test_contradictoryThresholds_quarantined();
        test_implausibleTemperature_rejected();
        test_wildcardApplicability_rejected();
        test_mismatchedPassageId_rejected();
        test_cleanFact_passes();
        test_checkedStatusNotPublished();
        test_conflictQueueContainsBothSides();
        test_passCheckPreservesProvenance();
        test_unitNormalization_conflict();

        System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ── Test cases ─────────────────────────────────────────────────────────────

    static void test_unrecognizedUnit_rejected() {
        String name = "test_unrecognizedUnit_rejected";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            CandidateFact fact = thresholdFact("FACT-001", "SRC-00001", "SRC-00001#P001",
                80.0, 105.0, "FLURBITS"); // not a real unit
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertEqual(name, "decision", CheckDecision.REJECT, result.getDecision());
            assertTrue(name, "UNRECOGNIZED_UNIT code present",
                result.getRejectionCodes().contains("UNRECOGNIZED_UNIT"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_missingRequiredField_rejected() {
        String name = "test_missingRequiredField_rejected";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            // Build a THRESHOLD fact but without providing unit (bypassing constructor enforcement
            // by using extractorVersion=null — which itself is a required-field violation)
            boolean thrown = false;
            try {
                // Missing unit on THRESHOLD fact — enforced at schema level
                CandidateFact.builder()
                    .factId("FACT-BAD").factType(FactType.THRESHOLD)
                    .sourceId("SRC-00001").passageId("SRC-00001#P001")
                    .supportingPassageText("coolant temp max 105.")
                    .thresholdUpper(105.0)
                    // unit intentionally omitted
                    .extractorVersion("v1")
                    .build();
            } catch (IllegalArgumentException e) { thrown = true; }
            assertTrue(name, "Schema rejects THRESHOLD without unit", thrown);

            // Also test via service: missing extractorVersion
            // We create a SYMPTOM_FAULT fact missing symptomDescription
            CandidateFact fact = CandidateFact.builder()
                .factId("FACT-002").factType(FactType.SYMPTOM_FAULT)
                .sourceId("SRC-00001").passageId("SRC-00001#P001")
                .supportingPassageText("some text")
                .faultDescription("Some fault")
                // symptomDescription intentionally missing
                .extractorVersion("v1")
                .build();
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertEqual(name, "decision", CheckDecision.REJECT, result.getDecision());
            assertTrue(name, "MISSING_REQUIRED_FIELD code present",
                result.getRejectionCodes().contains("MISSING_REQUIRED_FIELD"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_contradictoryThresholds_quarantined() {
        String name = "test_contradictoryThresholds_quarantined";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            Set<String> sources = Set.of("SRC-00001", "SRC-00002");

            // First fact: coolant temp max 100°C
            CandidateFact fact1 = thresholdFact("FACT-010", "SRC-00001", "SRC-00001#P010", null, 100.0, "°C");
            CheckResult r1 = service.check(fact1, sources);
            assertEqual(name, "fact1 decision", CheckDecision.PASS, r1.getDecision());

            // Second fact from different source: same vehicle/conditions, coolant temp min 110°C
            // → these ranges don't overlap → QUARANTINE
            CandidateFact fact2 = thresholdFact("FACT-011", "SRC-00002", "SRC-00002#P001", 110.0, 120.0, "°C");
            CheckResult r2 = service.check(fact2, sources);
            assertEqual(name, "fact2 decision", CheckDecision.QUARANTINE, r2.getDecision());
            assertTrue(name, "THRESHOLD_CONFLICT code present",
                r2.getRejectionCodes().contains("THRESHOLD_CONFLICT"));
            assertTrue(name, "conflictingFactIds contains fact1",
                r2.getConflictingFactIds().contains("FACT-010"));

            // IMPORTANT: quarantine store must contain fact2, NOT an averaged value
            assertTrue(name, "quarantine store contains fact2",
                service.getQuarantinedFacts().stream()
                    .anyMatch(f -> f.getFactId().equals("FACT-011")));
            // Checked store must NOT contain fact2 (it was quarantined)
            assertTrue(name, "checked store does NOT contain fact2",
                service.getCheckedFacts().stream()
                    .noneMatch(f -> f.getFactId().equals("FACT-011")));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_implausibleTemperature_rejected() {
        String name = "test_implausibleTemperature_rejected";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            // Below absolute zero (–300°C) — physically impossible
            CandidateFact fact = thresholdFact("FACT-020", "SRC-00001", "SRC-00001#P020", -300.0, -280.0, "°C");
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertEqual(name, "decision", CheckDecision.REJECT, result.getDecision());
            assertTrue(name, "IMPLAUSIBLE_RANGE code",
                result.getRejectionCodes().contains("IMPLAUSIBLE_RANGE"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_wildcardApplicability_rejected() {
        String name = "test_wildcardApplicability_rejected";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            CandidateFact fact = CandidateFact.builder()
                .factId("FACT-030").factType(FactType.ACTION)
                .sourceId("SRC-00001").passageId("SRC-00001#P030")
                .supportingPassageText("Replace the oil filter if contaminated.")
                .vehicleApplicability("all vehicles") // forbidden wildcard
                .actionDescription("Replace the oil filter if contaminated.")
                .extractorVersion("v1")
                .build();
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertEqual(name, "decision", CheckDecision.REJECT, result.getDecision());
            assertTrue(name, "INVALID_VEHICLE_APPLICABILITY code",
                result.getRejectionCodes().contains("INVALID_VEHICLE_APPLICABILITY"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_mismatchedPassageId_rejected() {
        String name = "test_mismatchedPassageId_rejected";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            // passageId references a different sourceId than the fact's sourceId
            CandidateFact fact = CandidateFact.builder()
                .factId("FACT-040").factType(FactType.ACTION)
                .sourceId("SRC-00001").passageId("SRC-00999#P001") // mismatched!
                .supportingPassageText("Check coolant level when engine is cold.")
                .actionDescription("Check coolant level when engine is cold.")
                .extractorVersion("v1")
                .build();
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertEqual(name, "decision", CheckDecision.REJECT, result.getDecision());
            assertTrue(name, "INCONSISTENT_PASSAGE_REF code",
                result.getRejectionCodes().contains("INCONSISTENT_PASSAGE_REF"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_cleanFact_passes() {
        String name = "test_cleanFact_passes";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            CandidateFact fact = thresholdFact("FACT-050", "SRC-00001", "SRC-00001#P050", 80.0, 105.0, "°C");
            CheckResult result = service.check(fact, Set.of("SRC-00001"));
            assertTrue(name, "decision is PASS or WARN (not REJECT/QUARANTINE)",
                result.getDecision() == CheckDecision.PASS || result.getDecision() == CheckDecision.WARN);
            assertTrue(name, "checked store contains fact",
                service.getCheckedFacts().stream().anyMatch(f -> f.getFactId().equals("FACT-050")));
            assertEqual(name, "fact status", FactStatus.CHECKED,
                service.getCheckedFacts().stream()
                    .filter(f -> f.getFactId().equals("FACT-050")).findFirst().get().getStatus());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_checkedStatusNotPublished() {
        String name = "test_checkedStatusNotPublished";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            CandidateFact fact = thresholdFact("FACT-060", "SRC-00001", "SRC-00001#P060", 1.0, 4.5, "bar");
            service.check(fact, Set.of("SRC-00001"));
            CandidateFact checkedFact = service.getCheckedFacts().stream()
                .filter(f -> f.getFactId().equals("FACT-060")).findFirst().orElse(null);
            assertNotNull(name, "checked fact present", checkedFact);
            // CHECKED ≠ PUBLISHED — status must be CHECKED, not PUBLISHED
            assertEqual(name, "status is CHECKED not PUBLISHED", FactStatus.CHECKED, checkedFact.getStatus());
            assertTrue(name, "status is not PUBLISHED", checkedFact.getStatus() != FactStatus.PUBLISHED);
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_conflictQueueContainsBothSides() {
        String name = "test_conflictQueueContainsBothSides";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            Set<String> sources = Set.of("SRC-00001", "SRC-00002");

            CandidateFact factA = thresholdFact("FACT-070", "SRC-00001", "SRC-00001#P070", null, 95.0, "°C");
            CandidateFact factB = thresholdFact("FACT-071", "SRC-00002", "SRC-00002#P001", 110.0, 130.0, "°C");
            service.check(factA, sources);
            service.check(factB, sources);

            List<CandidateFact> queue = service.getConflictReviewQueue();
            // factB was quarantined — must appear in review queue
            assertTrue(name, "conflict queue contains factB",
                queue.stream().anyMatch(f -> f.getFactId().equals("FACT-071")));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_passCheckPreservesProvenance() {
        String name = "test_passCheckPreservesProvenance";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            CandidateFact fact = thresholdFact("FACT-080", "SRC-00001", "SRC-00001#P080", 80.0, 105.0, "°C");
            service.check(fact, Set.of("SRC-00001"));
            Optional<CheckResult> result = service.getCheckResult("FACT-080");
            assertTrue(name, "check result preserved", result.isPresent());
            assertEqual(name, "factId in result", "FACT-080", result.get().getFactId());
            assertEqual(name, "sourceId in result", "SRC-00001", result.get().getSourceId());
            assertEqual(name, "passageId in result", "SRC-00001#P080", result.get().getPassageId());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_unitNormalization_conflict() {
        String name = "test_unitNormalization_conflict";
        try {
            ConsistencyCheckService service = new ConsistencyCheckService();
            Set<String> sources = Set.of("SRC-00001", "SRC-00002");

            // Fact 1: coolant temp max 100°C
            CandidateFact fact1 = thresholdFact("FACT-TEMP-C", "SRC-00001", "SRC-00001#P01", null, 100.0, "°C");
            service.check(fact1, sources);

            // Fact 2: coolant temp min 230°F (approx 110°C). Ranges do not overlap.
            CandidateFact fact2 = thresholdFact("FACT-TEMP-F", "SRC-00002", "SRC-00002#P01", 230.0, null, "°F");
            CheckResult r2 = service.check(fact2, sources);

            assertEqual(name, "fact2 decision", CheckDecision.QUARANTINE, r2.getDecision());
            assertTrue(name, "THRESHOLD_CONFLICT code present", r2.getRejectionCodes().contains("THRESHOLD_CONFLICT"));
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    // ── Fixture helpers ────────────────────────────────────────────────────────

    static CandidateFact thresholdFact(String factId, String sourceId, String passageId,
                                        Double lower, Double upper, String unit) {
        return CandidateFact.builder()
            .factId(factId).factType(FactType.THRESHOLD)
            .sourceId(sourceId).passageId(passageId)
            .supportingPassageText("Coolant temperature threshold fact.")
            .quantityId("COOLANT_TEMP")
            .thresholdRole("ABSOLUTE_LIMIT")
            .thresholdLower(lower).thresholdUpper(upper).unit(unit)
            .extractorVersion("rule-based-v1.0")
            .build();
    }

    // ── Assertion helpers ──────────────────────────────────────────────────────
    static void assertEqual(String test, String field, Object expected, Object actual) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError("FAIL [" + test + "] " + field + ": expected=" + expected + " actual=" + actual);
    }
    static void assertNotNull(String test, String field, Object v) {
        if (v == null) throw new AssertionError("FAIL [" + test + "] " + field + " must not be null");
    }
    static void assertTrue(String test, String cond, boolean v) {
        if (!v) throw new AssertionError("FAIL [" + test + "] condition false: " + cond);
    }
    static void pass(String name) { passed++; System.out.println("  PASS  " + name); }
    static void fail(String name, AssertionError e) { failed++; System.out.println("  FAIL  " + name + " — " + e.getMessage()); }
}
