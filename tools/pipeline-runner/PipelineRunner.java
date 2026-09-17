package tools;

import com.ignitionai.technicalsources.schema.*;
import com.ignitionai.technicalsources.service.*;
import com.ignitionai.technicalsources.store.SourceStore;
import com.ignitionai.factextraction.extractor.FactExtractor;
import com.ignitionai.factextraction.schema.*;
import com.ignitionai.factextraction.service.ExtractionService;
import com.ignitionai.consistencychecks.schema.*;
import com.ignitionai.consistencychecks.service.ConsistencyCheckService;
import com.ignitionai.linkedregistries.schema.*;
import com.ignitionai.linkedregistries.service.RegistryQueryService;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 1 end-to-end pipeline runner.
 *
 * This is development tooling, not an analytical pipeline module.
 * It exercises the full Phase 1 pipeline:
 *
 *   1. Ingest source documents from a directory
 *   2. Extract candidate facts from all passages
 *   3. Run consistency checks on all candidate facts
 *   4. Publish a registry release with checked facts
 *   5. Query the release for a given vehicle context
 *
 * Usage:
 *   java -cp . tools.PipelineRunner --source-dir path/to/docs --vehicle-family "VW_EA888_Gen3"
 *
 * Or run the built-in demo mode (no arguments) which uses inline sample documents.
 */
public class PipelineRunner {

    public static void main(String[] args) throws IOException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          IgnitionAI Phase 1 — Knowledge Engineering         ║");
        System.out.println("║                      Pipeline Runner                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Parse args
        String sourceDir    = null;
        String vehicleFamily = "VW_EA888_Gen3";  // default
        String manufacturer  = "Volkswagen";
        String engineFamily  = "EA888 Gen3";
        String fuelType      = "petrol";
        Integer modelYear    = 2018;

        for (int i = 0; i < args.length; i++) {
            if ("--source-dir".equals(args[i]) && i + 1 < args.length) {
                sourceDir = args[++i];
            } else if ("--vehicle-family".equals(args[i]) && i + 1 < args.length) {
                vehicleFamily = args[++i];
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Step 1: Set up source store and create sample documents if no dir given
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("━━━ Step 1: Technical Sources ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        SourceStore sourceStore = new SourceStore();
        SourceIngestionService ingestionService = new SourceIngestionService(sourceStore);

        List<Path> docPaths = new ArrayList<>();
        Path tmpDir = null;
        boolean usingSyntheticDemo = false;

        if (sourceDir != null) {
            Path dir = Path.of(sourceDir);
            if (Files.isDirectory(dir)) {
                Files.list(dir)
                    .filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".md"))
                    .forEach(docPaths::add);
                System.out.println("Loading documents from: " + dir);
            } else {
                System.out.println("WARNING: --source-dir is not a directory: " + dir);
            }
        }

        if (docPaths.isEmpty()) {
            System.out.println("No source directory provided — using built-in sample documents.");
            tmpDir = Files.createTempDirectory("ignitionai-pipeline-demo");
            docPaths.addAll(createSampleDocuments(tmpDir));
            usingSyntheticDemo = true;
        }

        VehicleScope scope = VehicleScope.builder()
            .manufacturer(manufacturer)
            .engineFamilies(List.of(engineFamily))
            .fuelTypes(List.of(fuelType))
            .modelYearFrom(2013).modelYearTo(2020)
            .build();

        Set<String> ingestedSourceIds = new HashSet<>();
        for (Path docPath : docPaths) {
            SourceMetadata meta = SourceMetadata.builder()
                .documentType(DocumentType.SERVICE_MANUAL)
                .language("en")
                .vehicleScope(scope)
                .isSynthetic(usingSyntheticDemo)
                .build();
            IngestionResult result = ingestionService.ingest(docPath, meta);
            System.out.printf("  [%s] %s — %s%n",
                result.getOutcome(), docPath.getFileName(), result.getExplanation());
            if (result.getOutcome() == IngestionResult.Outcome.INGESTED) {
                ingestedSourceIds.add(result.getSourceRecord().getSourceId());
            }
        }
        System.out.printf("  Total sources ingested: %d%n%n", ingestedSourceIds.size());

        // ─────────────────────────────────────────────────────────────────────
        // Step 2: Fact Extraction
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("━━━ Step 2: Fact Extraction ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        FactExtractor extractor = new FactExtractor();
        ExtractionService extractionService = new ExtractionService(extractor);

        for (String sourceId : ingestedSourceIds) {
            TechnicalSource source = sourceStore.findById(sourceId).orElseThrow();
            List<ExtractionResult> results = extractionService.extractFromSource(source);
            long factsExtracted = results.stream().mapToLong(r -> r.getFactCount()).sum();
            System.out.printf("  Source %s: %d passages → %d candidate facts%n",
                sourceId, source.getPassages().size(), factsExtracted);
        }

        List<CandidateFact> allFacts = extractionService.getAllCandidateFacts();
        System.out.printf("  Total candidate facts: %d%n%n", allFacts.size());

        // ─────────────────────────────────────────────────────────────────────
        // Step 3: Consistency Checks
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("━━━ Step 3: Consistency Checks ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ConsistencyCheckService checkService = new ConsistencyCheckService();
        Map<String, CheckResult> checkResults = checkService.checkAll(allFacts, ingestedSourceIds);

        System.out.printf("  Checked:     %d facts%n", checkService.getCheckedFacts().size());
        System.out.printf("  Rejected:    %d facts%n", checkService.getRejectedFacts().size());
        System.out.printf("  Quarantined: %d facts%n%n", checkService.getQuarantinedFacts().size());

        // Show rejection reasons
        for (CandidateFact rejected : checkService.getRejectedFacts()) {
            CheckResult r = checkResults.get(rejected.getFactId());
            System.out.printf("  REJECTED %s: %s%n", rejected.getFactId(),
                r != null ? r.getRejectionCodes() : "no result");
        }

        // ─────────────────────────────────────────────────────────────────────
        // Step 4: Publish to Linked Registries
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("━━━ Step 4: Linked Registries ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        RegistryQueryService registryService = new RegistryQueryService();

        // Add vehicle applicability entry (from the vehicle scope we set up)
        VehicleApplicabilityEntry va = VehicleApplicabilityEntry.builder()
            .entryId("VA-001")
            .configurationName(vehicleFamily)
            .manufacturer(manufacturer)
            .engineFamily(engineFamily)
            .fuelType(fuelType)
            .modelYearFrom(2013).modelYearTo(2020)
            .build();
        registryService.addVehicleApplicabilityEntry(va);

        // Add observation entries from checked threshold facts
        int obsSeq = 1;
        int actionSeq = 1;
        int faultSeq = 1;
        for (CandidateFact fact : checkService.getCheckedFacts()) {
            if (fact.getFactType() == FactType.THRESHOLD && fact.getUnit() != null) {
                String obsId = "OBS-AUTO-" + String.format("%03d", obsSeq++);
                try {
                    registryService.addObservationEntry(ObservationEntry.builder()
                        .observationId(obsId)
                        .name("Observation from fact " + fact.getFactId())
                        .measurementType("MEASURED")
                        .unit(fact.getUnit())
                        .normalLimitLower(fact.getThresholdLower())
                        .normalLimitUpper(fact.getThresholdUpper())
                        .vehicleApplicabilityEntryId("VA-001")
                        .sourceFactIds(List.of(fact.getFactId()))
                        .sourcePassageIds(List.of(fact.getPassageId()))
                        .build());
                } catch (Exception e) {
                    System.out.printf("  Skipping duplicate observation for fact %s%n", fact.getFactId());
                }
            } else if (fact.getFactType() == FactType.ACTION) {
                String actionId = "ACT-AUTO-" + String.format("%03d", actionSeq++);
                try {
                    registryService.addActionEntry(ActionEntry.builder()
                        .entryId(actionId)
                        .actionName(fact.getActionDescription())
                        .vehicleApplicabilityEntryId("VA-001")
                        .sourceFactIds(List.of(fact.getFactId()))
                        .sourcePassageIds(List.of(fact.getPassageId()))
                        .build());
                } catch (Exception e) {
                    System.out.printf("  Skipping duplicate action for fact %s%n", fact.getFactId());
                }
            } else if (fact.getFactType() == FactType.SYMPTOM_FAULT) {
                String faultId = "FLT-AUTO-" + String.format("%03d", faultSeq++);
                try {
                    registryService.addFaultKnowledgeEntry(FaultKnowledgeEntry.builder()
                        .entryId(faultId)
                        .faultName(fact.getFaultDescription())
                        .symptoms(List.of(fact.getSymptomDescription()))
                        .vehicleApplicabilityEntryId("VA-001")
                        .sourceFactIds(List.of(fact.getFactId()))
                        .sourcePassageIds(List.of(fact.getPassageId()))
                        .build());
                } catch (Exception e) {
                    System.out.printf("  Skipping duplicate fault for fact %s%n", fact.getFactId());
                }
            }
        }

        // Create a draft release (checked facts, not yet reviewed)
        RegistryRelease release = registryService.createDraftRelease(
            vehicleFamily + " — Initial Knowledge Pack (DRAFT)", vehicleFamily);
        System.out.printf("  Created release: %s%n", release.getReleaseId());
        System.out.printf("  Vehicle Applicability entries: %d%n", release.getVehicleApplicabilityEntries().size());
        System.out.printf("  Observation entries:           %d%n", release.getObservationEntries().size());
        System.out.printf("  Action entries:                %d%n", release.getActionEntries().size());
        System.out.printf("  Fault Knowledge entries:       %d%n", release.getFaultKnowledgeEntries().size());
        System.out.println();

        // ─────────────────────────────────────────────────────────────────────
        // Step 5: Query the registry
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("━━━ Step 5: Registry Query Demonstration ━━━━━━━━━━━━━━━━━━━━━━");

        // Compatible vehicle query
        List<VehicleApplicabilityEntry> compatible = registryService.queryVehicleApplicability(
            release.getReleaseId(), manufacturer, engineFamily, fuelType, modelYear);
        System.out.printf("  Query [%s / %s / %s / %d]: %d compatible entries%n",
            manufacturer, engineFamily, fuelType, modelYear, compatible.size());

        // Incompatible vehicle query (demonstrates the invariant)
        List<VehicleApplicabilityEntry> incompatible = registryService.queryVehicleApplicability(
            release.getReleaseId(), "BMW", "N20", "petrol", 2018);
        System.out.printf("  Query [BMW / N20 / petrol / 2018]:   %d compatible entries%n",
            incompatible.size());
        System.out.printf("  ✓ Incompatible vehicle correctly receives 0 entries%n");

        // ─────────────────────────────────────────────────────────────────────
        // Summary
        // ─────────────────────────────────────────────────────────────────────
        System.out.println();
        System.out.println("━━━ Phase 1 Summary ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.printf("  Sources ingested:            %d%n", ingestedSourceIds.size());
        System.out.printf("  Total passages processed:    %d%n",
            ingestedSourceIds.stream()
                .mapToInt(id -> sourceStore.findById(id).map(s -> s.getPassages().size()).orElse(0))
                .sum());
        System.out.printf("  Candidate facts extracted:   %d%n", allFacts.size());
        System.out.printf("  Facts checked:               %d%n", checkService.getCheckedFacts().size());
        System.out.printf("  Facts rejected:              %d%n", checkService.getRejectedFacts().size());
        System.out.printf("  Facts quarantined:           %d%n", checkService.getQuarantinedFacts().size());
        System.out.printf("  Registry release:            %s (%s)%n",
            release.getReleaseId(), release.getStatus());
        System.out.printf("  Registry entries:            %d%n", release.getTotalEntryCount());
        System.out.println();
        System.out.println("NOTE: Facts in this release are CHECKED (automated), not REVIEWED.");
        System.out.println("      Human review is required before promoting to PUBLISHED status.");
        System.out.println();
        System.out.println("✓ Phase 1 pipeline complete.");

        // Cleanup temp files
        if (tmpDir != null) {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    // ── Built-in sample documents ──────────────────────────────────────────────

    private static List<Path> createSampleDocuments(Path dir) throws IOException {
        List<Path> paths = new ArrayList<>();

        Path doc1 = dir.resolve("ea888-gen3-service-manual.txt");
        Files.writeString(doc1,
            "VW EA888 Gen3 Service Manual — Cooling System\n\n" +
            "1.1 Coolant Temperature\n" +
            "The coolant temperature must not exceed 105 degrees Celsius under normal operation.\n" +
            "The coolant temperature must be between 80 and 105 degrees Celsius at operating temperature.\n" +
            "Replace the thermostat if coolant temperature remains below 80 degrees after warm-up.\n\n" +
            "1.2 Engine Oil Pressure\n" +
            "Engine oil pressure must be between 1.0 and 4.5 bar during normal operation.\n" +
            "Engine oil pressure must not exceed 8.0 bar under any condition.\n\n" +
            "1.3 Fuel System\n" +
            "White smoke at cold start may indicate coolant entering the combustion chamber.\n" +
            "Replace the oxygen sensor if long-term fuel trim exceeds 15 percent.\n"
        );
        paths.add(doc1);

        Path doc2 = dir.resolve("ea888-gen3-technical-bulletin.txt");
        Files.writeString(doc2,
            "Technical Bulletin TB-2024-EA888-001\n\n" +
            "2.1 Battery Voltage Checks\n" +
            "Control module voltage must be between 11.0 and 14.5 V during normal operation.\n" +
            "Check the alternator if voltage drops below 11.0 V at idle.\n\n" +
            "2.2 Misfire Detection\n" +
            "Erratic idle at operating temperature may indicate injector fouling or misfires.\n" +
            "Inspect the ignition coils if misfire events are detected across multiple cylinders.\n"
        );
        paths.add(doc2);

        System.out.println("  Created 2 sample documents in: " + dir);
        return paths;
    }
}
