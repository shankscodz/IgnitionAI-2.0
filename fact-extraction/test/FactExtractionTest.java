package com.ignitionai.factextraction.test;

import com.ignitionai.factextraction.extractor.FactExtractor;
import com.ignitionai.factextraction.schema.*;
import com.ignitionai.factextraction.service.ExtractionService;
import com.ignitionai.technicalsources.schema.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Tests for the fact-extraction module.
 *
 * Verifies:
 * 1. Threshold facts are extracted with unit (never without)
 * 2. Range facts extract lower and upper bounds correctly
 * 3. Symptom-fault facts are extracted
 * 4. Action facts are extracted
 * 5. Absent information stays absent (null) — not inferred
 * 6. Multiple facts from one passage are supported
 * 7. Competing candidates from different sources are preserved
 * 8. THRESHOLD without unit is rejected at construction
 * 9. Short passage produces INSUFFICIENT_CONTENT, not a malformed fact
 */
public class FactExtractionTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== fact-extraction Tests ===\n");

        test_thresholdExtractionWithUnit();
        test_rangeExtractionWithBounds();
        test_symptomFaultExtraction();
        test_actionExtraction();
        test_absentInfoStaysAbsent();
        test_multipleFactsFromOnePassage();
        test_competingCandidatesFromDifferentSources();
        test_thresholdWithoutUnitRejected();
        test_shortPassageGivesInsufficientContent();
        test_noFactsFoundOutcome();
        test_extractionConfidenceSeparateFromSourceAuthority();

        System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ── Test cases ─────────────────────────────────────────────────────────────

    static void test_thresholdExtractionWithUnit() {
        String name = "test_thresholdExtractionWithUnit";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P001",
                "The coolant temperature must not exceed 105 degrees Celsius under normal operation.");
            ExtractionResult result = extractor.extract(passage, "VW EA888 Gen3");

            assertEqual(name, "outcome", ExtractionResult.Outcome.FACTS_EXTRACTED, result.getOutcome());
            assertTrue(name, "at least 1 fact", result.getFactCount() >= 1);

            CandidateFact thresholdFact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD).findFirst().orElse(null);
            assertNotNull(name, "threshold fact present", thresholdFact);
            assertNotNull(name, "unit present", thresholdFact.getUnit());
            assertTrue(name, "unit not blank", !thresholdFact.getUnit().isBlank());
            assertEqual(name, "status", FactStatus.CANDIDATE, thresholdFact.getStatus());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_rangeExtractionWithBounds() {
        String name = "test_rangeExtractionWithBounds";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P002",
                "Engine oil pressure must be between 1.0 and 4.5 bar during operation.");
            ExtractionResult result = extractor.extract(passage, null);

            CandidateFact rangeFact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD).findFirst().orElse(null);
            assertNotNull(name, "range fact present", rangeFact);
            assertNotNull(name, "lower bound present", rangeFact.getThresholdLower());
            assertNotNull(name, "upper bound present", rangeFact.getThresholdUpper());
            assertTrue(name, "lower < upper", rangeFact.getThresholdLower() < rangeFact.getThresholdUpper());
            assertNotNull(name, "unit present", rangeFact.getUnit());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_symptomFaultExtraction() {
        String name = "test_symptomFaultExtraction";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P003",
                "White smoke at cold start may indicate coolant entering the combustion chamber.");
            ExtractionResult result = extractor.extract(passage, null);

            CandidateFact symptomFact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.SYMPTOM_FAULT).findFirst().orElse(null);
            assertNotNull(name, "symptom-fault fact present", symptomFact);
            assertNotNull(name, "symptomDescription", symptomFact.getSymptomDescription());
            assertNotNull(name, "faultDescription", symptomFact.getFaultDescription());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_actionExtraction() {
        String name = "test_actionExtraction";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P004",
                "Replace the thermostat if coolant temperature remains below 80 degrees after warm-up.");
            ExtractionResult result = extractor.extract(passage, null);

            CandidateFact actionFact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.ACTION).findFirst().orElse(null);
            assertNotNull(name, "action fact present", actionFact);
            assertNotNull(name, "actionDescription", actionFact.getActionDescription());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_absentInfoStaysAbsent() {
        String name = "test_absentInfoStaysAbsent";
        try {
            // Pass null vehicleApplicabilityHint — extractor must not invent one
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00002", "SRC-00002#P001",
                "Coolant temperature must not exceed 105 degrees Celsius.");
            ExtractionResult result = extractor.extract(passage, null);  // no hint

            CandidateFact fact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD).findFirst().orElse(null);
            assertNotNull(name, "fact extracted", fact);
            assertNull(name, "vehicleApplicability must be null (not inferred)", fact.getVehicleApplicability());
            assertNull(name, "operatingConditions must be null (not inferred)", fact.getOperatingConditions());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_multipleFactsFromOnePassage() {
        String name = "test_multipleFactsFromOnePassage";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P005",
                "Coolant temperature must not exceed 105 degrees Celsius. " +
                "White smoke at cold start may indicate coolant entering the combustion chamber. " +
                "Replace the thermostat if temperature does not reach operating level.");
            ExtractionResult result = extractor.extract(passage, null);

            assertEqual(name, "outcome", ExtractionResult.Outcome.FACTS_EXTRACTED, result.getOutcome());
            assertTrue(name, "multiple facts produced", result.getFactCount() > 1);
            // All facts must reference the same passageId
            for (CandidateFact f : result.getFacts()) {
                assertEqual(name, "passageId matches", passage.getPassageId(), f.getPassageId());
            }
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_competingCandidatesFromDifferentSources() {
        String name = "test_competingCandidatesFromDifferentSources";
        try {
            // Two different sources with different thresholds for the same quantity
            FactExtractor extractor = new FactExtractor();
            Passage p1 = makePassage("SRC-00001", "SRC-00001#P010",
                "Coolant temperature must not exceed 100 degrees Celsius.");
            Passage p2 = makePassage("SRC-00002", "SRC-00002#P001",
                "Coolant temperature must not exceed 110 degrees Celsius.");

            ExtractionService service = new ExtractionService(extractor);

            TechnicalSource src1 = makeSource("SRC-00001", List.of(p1));
            TechnicalSource src2 = makeSource("SRC-00002", List.of(p2));

            service.extractFromSource(src1);
            service.extractFromSource(src2);

            List<CandidateFact> allFacts = service.getAllCandidateFacts();
            // Both competing facts must be preserved
            long thresholdCount = allFacts.stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD).count();
            assertTrue(name, "both competing facts preserved", thresholdCount >= 2);
            // They reference different sources
            long uniqueSources = allFacts.stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD)
                .map(CandidateFact::getSourceId).distinct().count();
            assertTrue(name, "facts from 2 different sources", uniqueSources >= 2);
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_thresholdWithoutUnitRejected() {
        String name = "test_thresholdWithoutUnitRejected";
        try {
            // Constructing a THRESHOLD fact without a unit must throw IllegalArgumentException
            boolean thrown = false;
            try {
                CandidateFact.builder()
                    .factId("FACT-BAD-001").factType(FactType.THRESHOLD)
                    .sourceId("SRC-00001").passageId("SRC-00001#P001")
                    .supportingPassageText("Coolant temp max 105.")
                    .thresholdUpper(105.0)
                    // unit intentionally omitted
                    .extractorVersion("rule-based-v1.0")
                    .build();
            } catch (IllegalArgumentException e) { thrown = true; }
            assertTrue(name, "IllegalArgumentException thrown for missing unit", thrown);
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_shortPassageGivesInsufficientContent() {
        String name = "test_shortPassageGivesInsufficientContent";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P099", "OK.");
            ExtractionResult result = extractor.extract(passage, null);
            assertEqual(name, "outcome", ExtractionResult.Outcome.INSUFFICIENT_CONTENT, result.getOutcome());
            assertEqual(name, "zero facts", 0, result.getFactCount());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_noFactsFoundOutcome() {
        String name = "test_noFactsFoundOutcome";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P100",
                "This document is a general introduction to the vehicle safety features. " +
                "Please read all sections carefully before commencing any diagnostic work.");
            ExtractionResult result = extractor.extract(passage, null);
            assertEqual(name, "outcome", ExtractionResult.Outcome.NO_FACTS_FOUND, result.getOutcome());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_extractionConfidenceSeparateFromSourceAuthority() {
        String name = "test_extractionConfidenceSeparateFromSourceAuthority";
        try {
            FactExtractor extractor = new FactExtractor();
            Passage passage = makePassage("SRC-00001", "SRC-00001#P011",
                "Coolant temperature must not exceed 105 degrees Celsius during normal operation.");
            ExtractionResult result = extractor.extract(passage, null);

            CandidateFact fact = result.getFacts().stream()
                .filter(f -> f.getFactType() == FactType.THRESHOLD).findFirst().orElse(null);
            assertNotNull(name, "fact present", fact);
            // extractionConfidence is present and distinct from source authority (which is in the source record)
            assertNotNull(name, "extractionConfidence present", fact.getExtractionConfidence());
            assertTrue(name, "extractorVersion recorded", !fact.getExtractorVersion().isBlank());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    // ── Fixture helpers ────────────────────────────────────────────────────────

    static Passage makePassage(String sourceId, String passageId, String text) {
        return Passage.builder().passageId(passageId).sourceId(sourceId).text(text).build();
    }

    static TechnicalSource makeSource(String sourceId, List<Passage> passages) {
        return TechnicalSource.builder()
            .sourceId(sourceId).title("Test Source " + sourceId)
            .documentType(com.ignitionai.technicalsources.schema.DocumentType.SERVICE_MANUAL)
            .retrievalDate(LocalDate.now())
            .documentHash("fakehash" + sourceId)
            .ingestionStatus(com.ignitionai.technicalsources.schema.IngestionStatus.INGESTED)
            .passages(passages)
            .build();
    }

    // ── Assertion helpers ──────────────────────────────────────────────────────

    static void assertEqual(String test, String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("FAIL [" + test + "] " + field + ": expected=" + expected + " actual=" + actual);
    }

    static void assertNotNull(String test, String field, Object value) {
        if (value == null) throw new AssertionError("FAIL [" + test + "] " + field + " must not be null");
    }

    static void assertNull(String test, String field, Object value) {
        if (value != null) throw new AssertionError("FAIL [" + test + "] " + field + " must be null, was: " + value);
    }

    static void assertTrue(String test, String condition, boolean value) {
        if (!value) throw new AssertionError("FAIL [" + test + "] condition false: " + condition);
    }

    static void pass(String name) { passed++; System.out.println("  PASS  " + name); }
    static void fail(String name, AssertionError e) { failed++; System.out.println("  FAIL  " + name + " — " + e.getMessage()); }
}
