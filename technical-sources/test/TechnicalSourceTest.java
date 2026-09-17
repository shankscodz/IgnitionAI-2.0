package com.ignitionai.technicalsources.test;

import com.ignitionai.technicalsources.schema.*;
import com.ignitionai.technicalsources.service.SourceIngestionService;
import com.ignitionai.technicalsources.service.SourceMetadata;
import com.ignitionai.technicalsources.store.SourceStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Tests for the technical-sources module.
 *
 * Verifies:
 * 1. Successful ingestion and passage extraction
 * 2. Duplicate detection by hash (same bytes → DUPLICATE, no re-storage)
 * 3. Revision chain handling (predecessor link, chain retrieval)
 * 4. Unreadable file handling (binary content → UNREADABLE)
 * 5. Missing metadata remains null (not inferred)
 * 6. Passage retrieval by passageId
 *
 * Run via the pipeline runner or directly as a Java main.
 */
public class TechnicalSourceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("=== technical-sources Tests ===\n");

        SourceStore store = new SourceStore();
        SourceIngestionService service = new SourceIngestionService(store);

        // Create temp directory for test files
        Path tmpDir = Files.createTempDirectory("ignitionai-test-sources");

        try {
            test_successfulIngestion(service, store, tmpDir);
            test_duplicateDetection(service, store, tmpDir);
            test_revisionChain(service, store, tmpDir);
            test_unreadableFile(service, store, tmpDir);
            test_missingMetadataRemainsNull(service, store, tmpDir);
            test_passageRetrieval(service, store, tmpDir);
            test_notFound(service, store);
        } finally {
            // Cleanup temp files
            Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }

        System.out.printf("%n=== Results: %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ── Test cases ────────────────────────────────────────────────────────────

    static void test_successfulIngestion(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_successfulIngestion";
        try {
            Path doc = writeTextFile(tmpDir, "service-manual-1.txt",
                "VW EA888 Gen3 Service Manual\n" +
                "1.1 Coolant System\n" +
                "The coolant temperature must not exceed 105 degrees Celsius under normal operation.\n" +
                "Replace the thermostat if coolant temperature remains below 80 degrees after 10 minutes warm-up.\n" +
                "1.2 Engine Oil\n" +
                "Engine oil pressure at idle must be above 1.0 bar.\n");

            SourceMetadata meta = SourceMetadata.builder()
                .title("VW EA888 Gen3 Service Manual")
                .publisher("Volkswagen AG")
                .documentType(DocumentType.SERVICE_MANUAL)
                .language("en")
                .vehicleScope(VehicleScope.builder()
                    .manufacturer("Volkswagen")
                    .engineFamilies(List.of("EA888 Gen3"))
                    .build())
                .build();

            IngestionResult result = service.ingest(doc, meta);

            assertEqual(name, "outcome", IngestionResult.Outcome.INGESTED, result.getOutcome());
            assertNotNull(name, "sourceRecord", result.getSourceRecord());
            assertNotNull(name, "passages", result.getSourceRecord().getPassages());
            assertTrue(name, "at least 1 passage", !result.getSourceRecord().getPassages().isEmpty());
            assertEqual(name, "publisher", "Volkswagen AG", result.getSourceRecord().getPublisher());
            assertEqual(name, "status", IngestionStatus.INGESTED, result.getSourceRecord().getIngestionStatus());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_duplicateDetection(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_duplicateDetection";
        try {
            // Write the same content to two differently-named files
            String content = "Duplicate test document content - EA211 engine specifications.";
            Path doc1 = writeTextFile(tmpDir, "dup-doc-a.txt", content);
            Path doc2 = writeTextFile(tmpDir, "dup-doc-b.txt", content);

            SourceMetadata meta = SourceMetadata.builder()
                .title("Duplicate Doc A").documentType(DocumentType.TECHNICAL_BULLETIN).build();
            IngestionResult first = service.ingest(doc1, meta);

            SourceMetadata meta2 = SourceMetadata.builder()
                .title("Duplicate Doc B").documentType(DocumentType.TECHNICAL_BULLETIN).build();
            IngestionResult second = service.ingest(doc2, meta2);

            assertEqual(name, "first outcome", IngestionResult.Outcome.INGESTED, first.getOutcome());
            assertEqual(name, "second outcome", IngestionResult.Outcome.DUPLICATE, second.getOutcome());
            assertEqual(name, "rejection code", "DUPLICATE_HASH", second.getRejectionCode());
            // Store should have same sourceId for both
            assertEqual(name, "sourceId matches original", first.getSourceRecord().getSourceId(),
                        second.getSourceRecord().getSourceId());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_revisionChain(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_revisionChain";
        try {
            Path v1 = writeTextFile(tmpDir, "bulletin-v1.txt",
                "Technical Bulletin TB-2024-001 Rev 1\nCoolant system: max temp 100C.");
            Path v2 = writeTextFile(tmpDir, "bulletin-v2.txt",
                "Technical Bulletin TB-2024-001 Rev 2\nCoolant system: max temp 105C (corrected).");

            SourceMetadata metaV1 = SourceMetadata.builder()
                .title("TB-2024-001 Rev 1").documentType(DocumentType.TECHNICAL_BULLETIN).build();
            IngestionResult r1 = service.ingest(v1, metaV1);

            SourceMetadata metaV2 = SourceMetadata.builder()
                .title("TB-2024-001 Rev 2").documentType(DocumentType.TECHNICAL_BULLETIN).build();
            IngestionResult r2 = service.ingestRevision(v2, metaV2, r1.getSourceRecord().getSourceId());

            assertEqual(name, "r1 outcome", IngestionResult.Outcome.INGESTED, r1.getOutcome());
            assertEqual(name, "r2 outcome", IngestionResult.Outcome.INGESTED, r2.getOutcome());

            // Check predecessor link
            assertEqual(name, "predecessor link",
                r1.getSourceRecord().getSourceId(),
                r2.getSourceRecord().getPredecessorSourceId());

            // Check revision chain retrieval
            List<TechnicalSource> chain = store.getRevisionChain(r2.getSourceRecord().getSourceId());
            assertEqual(name, "chain length", 2, chain.size());
            assertEqual(name, "chain[0] is v1", r1.getSourceRecord().getSourceId(), chain.get(0).getSourceId());
            assertEqual(name, "chain[1] is v2", r2.getSourceRecord().getSourceId(), chain.get(1).getSourceId());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_unreadableFile(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_unreadableFile";
        try {
            // Write a file with >10% non-printable bytes (simulating binary/PDF/scan)
            byte[] binaryContent = new byte[500];
            for (int i = 0; i < binaryContent.length; i++) {
                binaryContent[i] = (byte) (i % 5 == 0 ? 0x01 : 0x41); // 20% non-printable
            }
            Path binaryFile = tmpDir.resolve("scan.pdf");
            Files.write(binaryFile, binaryContent);

            SourceMetadata meta = SourceMetadata.builder()
                .title("Scanned Document").documentType(DocumentType.SERVICE_MANUAL).build();
            IngestionResult result = service.ingest(binaryFile, meta);

            assertEqual(name, "outcome", IngestionResult.Outcome.UNREADABLE, result.getOutcome());
            assertNotNull(name, "rejectionCode", result.getRejectionCode());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_missingMetadataRemainsNull(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_missingMetadataRemainsNull";
        try {
            Path doc = writeTextFile(tmpDir, "minimal-doc.txt",
                "Minimal document with no publisher or date information.");

            // Provide only the required minimum — no publisher, no dates, no vehicleScope
            SourceMetadata meta = SourceMetadata.builder()
                .title("Minimal Document")
                .documentType(DocumentType.OTHER)
                .build();

            IngestionResult result = service.ingest(doc, meta);
            assertEqual(name, "outcome", IngestionResult.Outcome.INGESTED, result.getOutcome());

            TechnicalSource src = result.getSourceRecord();
            assertNull(name, "publisher should be null", src.getPublisher());
            assertNull(name, "publicationDate should be null", src.getPublicationDate());
            assertNull(name, "revisionDate should be null", src.getRevisionDate());
            assertNull(name, "vehicleScope should be null", src.getVehicleScope());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_passageRetrieval(SourceIngestionService service, SourceStore store, Path tmpDir) throws IOException {
        String name = "test_passageRetrieval";
        try {
            Path doc = writeTextFile(tmpDir, "passage-doc.txt",
                "1.1 Fuel System\n" +
                "Fuel pressure at idle should be between 3.5 and 4.5 bar.\n" +
                "1.2 Ignition Timing\n" +
                "Ignition timing at idle: 5–10 degrees BTDC.\n");

            SourceMetadata meta = SourceMetadata.builder()
                .title("Fuel and Ignition Manual").documentType(DocumentType.SERVICE_MANUAL).build();
            IngestionResult result = service.ingest(doc, meta);

            String sourceId = result.getSourceRecord().getSourceId();
            List<Passage> passages = result.getSourceRecord().getPassages();
            assertTrue(name, "passages exist", !passages.isEmpty());

            // Retrieve first passage by its passageId
            String firstPassageId = passages.get(0).getPassageId();
            java.util.Optional<Passage> retrieved = store.findPassage(sourceId, firstPassageId);
            assertTrue(name, "passage retrievable", retrieved.isPresent());
            assertTrue(name, "passage text non-empty", !retrieved.get().getText().isEmpty());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    static void test_notFound(SourceIngestionService service, SourceStore store) {
        String name = "test_notFound";
        try {
            SourceMetadata meta = SourceMetadata.builder()
                .title("Ghost File").documentType(DocumentType.OTHER).build();
            IngestionResult result = service.ingest(Path.of("/nonexistent/path/file.txt"), meta);
            assertEqual(name, "outcome", IngestionResult.Outcome.NOT_FOUND, result.getOutcome());
            pass(name);
        } catch (AssertionError e) { fail(name, e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static Path writeTextFile(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    static void assertEqual(String test, String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("FAIL [" + test + "] " + field + ": expected=" + expected + " actual=" + actual);
        }
    }

    static void assertNotNull(String test, String field, Object value) {
        if (value == null) throw new AssertionError("FAIL [" + test + "] " + field + " must not be null");
    }

    static void assertNull(String test, String field, Object value) {
        if (value != null) throw new AssertionError("FAIL [" + test + "] " + field + " must be null but was: " + value);
    }

    static void assertTrue(String test, String condition, boolean value) {
        if (!value) throw new AssertionError("FAIL [" + test + "] condition false: " + condition);
    }

    static void pass(String name) {
        passed++;
        System.out.println("  PASS  " + name);
    }

    static void fail(String name, AssertionError e) {
        failed++;
        System.out.println("  FAIL  " + name + " — " + e.getMessage());
    }
}
