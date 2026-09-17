package com.ignitionai.linkedregistries.test;

import com.ignitionai.linkedregistries.schema.*;
import com.ignitionai.linkedregistries.service.RegistryQueryService;

import java.util.List;
import java.util.Optional;

/**
 * Tests for the linked-registries module.
 *
 * Verifies Phase 1 exit demonstration:
 * 1. Source passage → candidate fact → check result → published registry entry → retrieval
 * 2. Compatible vehicle receives the entry
 * 3. INCOMPATIBLE vehicle does NOT receive the entry (key invariant)
 * 4. Cross-registry references are validated (unknown observationId → rejected)
 * 5. Duplicate entryId → rejected
 * 6. Release pinning: queries use release snapshot, not live draft
 * 7. DRAFT vs PUBLISHED release distinction
 * 8. Observation lookup by observationId
 */
public class LinkedRegistriesTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== linked-registries Tests ===\n");

        test_vehicleApplicabilityLookup_compatibleVehicle();
        test_vehicleApplicabilityLookup_incompatibleVehicle();
        test_observationLookupByObservationId();
        test_faultKnowledgeQuery_byVehicleApplicabilityId();
        test_crossRegistryReference_unknownObservationId_rejected();
        test_duplicateEntryId_rejected();
        test_releasePinning_snapshotNotLiveDraft();
        test_draftVsPublishedReleaseStatus();
        test_fullTraceabilityChain();

        System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ── Test cases ─────────────────────────────────────────────────────────────

    static void test_vehicleApplicabilityLookup_compatibleVehicle() {
        String name = "test_vehicleApplicabilityLookup_compatibleVehicle";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-001").configurationName("VW EA888 Gen3 Petrol")
                .manufacturer("Volkswagen").engineFamily("EA888 Gen3").fuelType("petrol")
                .modelYearFrom(2013).modelYearTo(2020)
                .sourceFactIds(List.of("FACT-001")).sourcePassageIds(List.of("SRC-00001#P001"))
                .build());

            RegistryRelease release = service.createPublishedRelease("Test Release", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of("FACT-001"));

            // Query with compatible vehicle
            List<VehicleApplicabilityEntry> results = service.queryVehicleApplicability(
                release.getReleaseId(), "Volkswagen", "EA888 Gen3", "petrol", 2018);
            assertTrue(name, "compatible vehicle gets 1 entry", results.size() == 1);
            assertEqual(name, "correct entryId", "VA-001", results.get(0).getEntryId());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_vehicleApplicabilityLookup_incompatibleVehicle() {
        String name = "test_vehicleApplicabilityLookup_incompatibleVehicle";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-002").configurationName("VW EA888 Gen3 Petrol")
                .manufacturer("Volkswagen").engineFamily("EA888 Gen3").fuelType("petrol")
                .modelYearFrom(2013).modelYearTo(2020)
                .build());

            RegistryRelease release = service.createPublishedRelease("Test Release", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            // Query with INCOMPATIBLE vehicle (different engine family)
            List<VehicleApplicabilityEntry> results = service.queryVehicleApplicability(
                release.getReleaseId(), "Toyota", "1NZ-FE", "petrol", 2018);
            assertTrue(name, "INCOMPATIBLE vehicle gets ZERO entries", results.isEmpty());

            // Query with wrong fuel type
            List<VehicleApplicabilityEntry> dieselResults = service.queryVehicleApplicability(
                release.getReleaseId(), "Volkswagen", "EA888 Gen3", "diesel", 2018);
            assertTrue(name, "wrong fuel type gets ZERO entries", dieselResults.isEmpty());

            // Query with out-of-range year
            List<VehicleApplicabilityEntry> outOfRangeResults = service.queryVehicleApplicability(
                release.getReleaseId(), "Volkswagen", "EA888 Gen3", "petrol", 2022);
            assertTrue(name, "out-of-year-range gets ZERO entries", outOfRangeResults.isEmpty());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_observationLookupByObservationId() {
        String name = "test_observationLookupByObservationId";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addObservationEntry(ObservationEntry.builder()
                .observationId("OBS-COOLANT-TEMP")
                .name("Engine Coolant Temperature")
                .measurementType("MEASURED")
                .unit("°C")
                .definition("Temperature of the engine coolant circuit as reported by the ECT sensor.")
                .normalLimitLower(80.0).normalLimitUpper(105.0)
                .pidOrDid("PID 05")
                .build());

            RegistryRelease release = service.createPublishedRelease("Test", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            Optional<ObservationEntry> found = service.queryObservation(
                release.getReleaseId(), "OBS-COOLANT-TEMP");
            assertTrue(name, "observation found", found.isPresent());
            assertEqual(name, "unit", "°C", found.get().getUnit());
            assertEqual(name, "measurementType", "MEASURED", found.get().getMeasurementType());

            Optional<ObservationEntry> notFound = service.queryObservation(
                release.getReleaseId(), "OBS-DOES-NOT-EXIST");
            assertTrue(name, "unknown observationId returns empty", notFound.isEmpty());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_faultKnowledgeQuery_byVehicleApplicabilityId() {
        String name = "test_faultKnowledgeQuery_byVehicleApplicabilityId";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-010").configurationName("VW EA888 Gen3").manufacturer("Volkswagen").build());
            service.addObservationEntry(ObservationEntry.builder()
                .observationId("OBS-COOLANT-TEMP").name("Coolant Temp")
                .measurementType("MEASURED").unit("°C").build());
            service.addFaultKnowledgeEntry(FaultKnowledgeEntry.builder()
                .entryId("FK-001").faultName("Thermostat Failure")
                .vehicleApplicabilityEntryId("VA-010")
                .relevantObservationIds(List.of("OBS-COOLANT-TEMP"))
                .symptoms(List.of("Engine takes too long to reach operating temperature"))
                .build());

            RegistryRelease release = service.createPublishedRelease("Test", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            List<FaultKnowledgeEntry> faults = service.queryFaultKnowledge(release.getReleaseId(), "VA-010");
            assertTrue(name, "one fault entry found", faults.size() == 1);
            assertEqual(name, "fault name", "Thermostat Failure", faults.get(0).getFaultName());

            // Different vehicleApplicabilityEntryId → no results
            List<FaultKnowledgeEntry> other = service.queryFaultKnowledge(release.getReleaseId(), "VA-999");
            assertTrue(name, "unknown VA ID gets no fault entries", other.isEmpty());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_crossRegistryReference_unknownObservationId_rejected() {
        String name = "test_crossRegistryReference_unknownObservationId_rejected";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-020").configurationName("VW EA888 Gen3").build());
            // Try adding an ActionEntry referencing an observation not yet in the draft store
            boolean thrown = false;
            try {
                service.addActionEntry(ActionEntry.builder()
                    .entryId("ACT-001").actionName("Check coolant temperature")
                    .vehicleApplicabilityEntryId("VA-020")
                    .relatedObservationIds(List.of("OBS-DOES-NOT-EXIST")) // unknown!
                    .build());
            } catch (IllegalArgumentException e) { thrown = true; }
            assertTrue(name, "Unknown observationId reference throws IllegalArgumentException", thrown);
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_duplicateEntryId_rejected() {
        String name = "test_duplicateEntryId_rejected";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-DUP").configurationName("First Entry").build());
            boolean thrown = false;
            try {
                service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                    .entryId("VA-DUP").configurationName("Second Entry — duplicate ID").build());
            } catch (IllegalArgumentException e) { thrown = true; }
            assertTrue(name, "Duplicate entryId throws IllegalArgumentException", thrown);
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_releasePinning_snapshotNotLiveDraft() {
        String name = "test_releasePinning_snapshotNotLiveDraft";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-PIN-001").configurationName("EA888 Pre-Release Entry")
                .manufacturer("Volkswagen").engineFamily("EA888 Gen3").fuelType("petrol").build());

            // Create Release A with one entry
            RegistryRelease releaseA = service.createPublishedRelease("Release A", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            // Add another entry to the draft AFTER Release A
            service.addVehicleApplicabilityEntry(VehicleApplicabilityEntry.builder()
                .entryId("VA-PIN-002").configurationName("EA888 Post-Release Entry")
                .manufacturer("Volkswagen").engineFamily("EA888 Gen3").fuelType("petrol").build());

            // Create Release B with both entries
            RegistryRelease releaseB = service.createPublishedRelease("Release B", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            // Release A should still have only 1 entry (snapshot at time of creation)
            List<VehicleApplicabilityEntry> aResults = service.queryVehicleApplicability(
                releaseA.getReleaseId(), "Volkswagen", "EA888 Gen3", "petrol", null);
            assertEqual(name, "Release A has 1 entry (snapshot)", 1, aResults.size());

            // Release B has both entries
            List<VehicleApplicabilityEntry> bResults = service.queryVehicleApplicability(
                releaseB.getReleaseId(), "Volkswagen", "EA888 Gen3", "petrol", null);
            assertEqual(name, "Release B has 2 entries", 2, bResults.size());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_draftVsPublishedReleaseStatus() {
        String name = "test_draftVsPublishedReleaseStatus";
        try {
            RegistryQueryService service = new RegistryQueryService();
            service.addObservationEntry(ObservationEntry.builder()
                .observationId("OBS-DRAFT").name("Draft Obs")
                .measurementType("MEASURED").unit("°C").build());

            RegistryRelease draft = service.createDraftRelease("Draft Release", "EA888 Gen3");
            RegistryRelease published = service.createPublishedRelease("Published Release", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of());

            assertEqual(name, "draft status", RegistryRelease.ReleaseStatus.DRAFT, draft.getStatus());
            assertEqual(name, "published status", RegistryRelease.ReleaseStatus.PUBLISHED, published.getStatus());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_fullTraceabilityChain() {
        String name = "test_fullTraceabilityChain (Phase 1 exit demo)";
        try {
            // Full Phase 1 exit demonstration:
            // Source passage → candidate fact → check result → published registry entry → retrieval
            RegistryQueryService service = new RegistryQueryService();

            // Step: Add observation entry derived from a REVIEWED fact
            ObservationEntry obs = ObservationEntry.builder()
                .observationId("OBS-COOLANT-TEMP-FINAL")
                .name("Engine Coolant Temperature")
                .measurementType("MEASURED").unit("°C")
                .definition("Temperature of engine coolant from ECT sensor.")
                .normalLimitLower(80.0).normalLimitUpper(105.0)
                .pidOrDid("PID 05")
                .sourceFactIds(List.of("FACT-001"))             // ← traceability to fact
                .sourcePassageIds(List.of("SRC-00001#P001"))    // ← traceability to passage
                .build();
            service.addObservationEntry(obs);

            // Step: Add vehicle applicability entry
            VehicleApplicabilityEntry va = VehicleApplicabilityEntry.builder()
                .entryId("VA-FINAL-001").configurationName("VW EA888 Gen3 Petrol")
                .manufacturer("Volkswagen").engineFamily("EA888 Gen3").fuelType("petrol")
                .modelYearFrom(2013).modelYearTo(2020)
                .sourceFactIds(List.of("FACT-002"))
                .sourcePassageIds(List.of("SRC-00001#P002"))
                .build();
            service.addVehicleApplicabilityEntry(va);

            // Step: Create published release
            RegistryRelease release = service.createPublishedRelease(
                "EA888 Gen3 Initial Knowledge Pack v1.0", "EA888 Gen3", "reviewer1", "APPROVED", java.util.Set.of("FACT-001", "FACT-002"));

            // Step: Query for matching vehicle — must receive the entry
            List<VehicleApplicabilityEntry> matchResults = service.queryVehicleApplicability(
                release.getReleaseId(), "Volkswagen", "EA888 Gen3", "petrol", 2018);
            assertEqual(name, "matching vehicle gets 1 VA entry", 1, matchResults.size());

            // Step: Query for NON-matching vehicle — must receive NOTHING
            List<VehicleApplicabilityEntry> noMatchResults = service.queryVehicleApplicability(
                release.getReleaseId(), "BMW", "N20", "petrol", 2018);
            assertTrue(name, "non-matching vehicle gets ZERO entries", noMatchResults.isEmpty());

            // Step: Verify traceability links are preserved in the release
            VehicleApplicabilityEntry entry = matchResults.get(0);
            assertTrue(name, "sourceFactIds preserved", !entry.getSourceFactIds().isEmpty());
            assertTrue(name, "sourcePassageIds preserved", !entry.getSourcePassageIds().isEmpty());
            assertTrue(name, "sourceFactIds link to FACT-002", entry.getSourceFactIds().contains("FACT-002"));

            // Step: Verify observation lookup
            Optional<ObservationEntry> obsResult = service.queryObservation(
                release.getReleaseId(), "OBS-COOLANT-TEMP-FINAL");
            assertTrue(name, "observation retrievable by ID", obsResult.isPresent());
            assertEqual(name, "observation unit", "°C", obsResult.get().getUnit());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    // ── Assertion helpers ──────────────────────────────────────────────────────
    static void assertEqual(String test, String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("FAIL [" + test + "] " + field + ": expected=" + expected + " actual=" + actual);
    }
    static void assertTrue(String test, String cond, boolean v) {
        if (!v) throw new AssertionError("FAIL [" + test + "] condition false: " + cond);
    }
    static void pass(String name) { passed++; System.out.println("  PASS  " + name); }
    static void fail(String name, AssertionError e) { failed++; System.out.println("  FAIL  " + name + " — " + e.getMessage()); }
}
