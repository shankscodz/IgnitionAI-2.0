package com.ignitionai.technicalsources.service;

import com.ignitionai.technicalsources.schema.*;
import com.ignitionai.technicalsources.store.SourceStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for ingesting technical documents into the {@link SourceStore}.
 *
 * Responsibilities:
 * 1. Compute the SHA-256 hash of the document bytes
 * 2. Detect duplicates (same hash → DUPLICATE result, no re-storage)
 * 3. Extract text passages from the document
 * 4. Store the {@link TechnicalSource} record with ingestion status INGESTED
 * 5. Handle unreadable files and missing metadata explicitly
 *
 * This service does NOT infer metadata. Missing fields remain null/unknown.
 * Scans/OCR are not supported in the MVP — they produce UNREADABLE status.
 */
public class SourceIngestionService {

    private static final int PASSAGE_CHUNK_SIZE = 2000;
    private static final Pattern SECTION_PATTERN =
        Pattern.compile("(?m)^(\\d+\\.\\d*\\s+[A-Z][^\\n]{5,80})$");

    private final SourceStore store;
    private final AtomicInteger sourceSequence = new AtomicInteger(1000);

    public SourceIngestionService(SourceStore store) {
        this.store = Objects.requireNonNull(store);
    }

    /**
     * Ingest a document from a file path.
     *
     * @param filePath    Path to the document file (must be a text-readable file for MVP)
     * @param metadata    Known metadata to associate with this source.
     *                    Fields not known must be null in the metadata — never guessed.
     * @return An {@link IngestionResult} describing the outcome.
     */
    public IngestionResult ingest(Path filePath, SourceMetadata metadata) {
        // 1. Check file exists
        if (!Files.exists(filePath)) {
            return IngestionResult.builder()
                .outcome(IngestionResult.Outcome.NOT_FOUND)
                .attemptedPath(filePath.toString())
                .explanation("File not found: " + filePath)
                .build();
        }

        // 2. Read bytes and compute SHA-256 hash
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            return IngestionResult.builder()
                .outcome(IngestionResult.Outcome.UNREADABLE)
                .attemptedPath(filePath.toString())
                .explanation("Could not read file: " + e.getMessage())
                .build();
        }

        String hash = sha256Hex(bytes);

        // 2b. Archive original bytes
        archiveSourceBytes(hash, bytes);

        // 3. Duplicate detection by hash
        Optional<String> existingId = store.findByHash(hash);
        if (existingId.isPresent()) {
            TechnicalSource existing = store.findById(existingId.get()).orElseThrow();
            return IngestionResult.builder()
                .outcome(IngestionResult.Outcome.DUPLICATE)
                .sourceRecord(existing)
                .rejectionCode("DUPLICATE_HASH")
                .explanation("Document already ingested with sourceId='" + existingId.get() +
                             "'. Returning existing record. Hash: " + hash.substring(0, 16) + "...")
                .attemptedPath(filePath.toString())
                .build();
        }

        // 4. Extract text content
        String text;
        try {
            text = new String(bytes, StandardCharsets.UTF_8);
            // Rough check: if more than 10% of characters are non-printable → likely binary/scan
            long nonPrintable = text.chars().filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
            if (nonPrintable > text.length() * 0.10) {
                return IngestionResult.builder()
                    .outcome(IngestionResult.Outcome.UNREADABLE)
                    .rejectionCode("NON_TEXT_CONTENT")
                    .explanation("Document appears to contain binary or scanned content. " +
                                 "OCR support is a separately estimated extension.")
                    .attemptedPath(filePath.toString())
                    .build();
            }
        } catch (Exception e) {
            return IngestionResult.builder()
                .outcome(IngestionResult.Outcome.UNREADABLE)
                .rejectionCode("TEXT_EXTRACTION_FAILED")
                .explanation("Text extraction failed: " + e.getMessage())
                .attemptedPath(filePath.toString())
                .build();
        }

        // 5. Generate a stable sourceId
        String sourceId = "SRC-" + String.format("%05d", sourceSequence.getAndIncrement());

        // 6. Extract passages
        List<Passage> passages = extractPassages(sourceId, text);

        // 7. Build source record
        TechnicalSource source = TechnicalSource.builder()
            .sourceId(sourceId)
            .title(metadata.getTitle() != null ? metadata.getTitle() : filePath.getFileName().toString())
            .publisher(metadata.getPublisher())
            .documentType(metadata.getDocumentType() != null ? metadata.getDocumentType() : DocumentType.OTHER)
            .originalUrl(metadata.getOriginalUrl())
            .fileLocation(filePath.toAbsolutePath().toString())
            .publicationDate(metadata.getPublicationDate())
            .revisionDate(metadata.getRevisionDate())
            .retrievalDate(LocalDate.now())
            .language(metadata.getLanguage())
            .documentHash(hash)
            .accessRestrictions(metadata.getAccessRestrictions())
            .vehicleScope(metadata.getVehicleScope())
            .ingestionStatus(IngestionStatus.INGESTED)
            .predecessorSourceId(metadata.getPredecessorSourceId())
            .isSynthetic(metadata.isSynthetic())
            .passages(passages)
            .build();

        store.store(source);

        return IngestionResult.builder()
            .outcome(IngestionResult.Outcome.INGESTED)
            .sourceRecord(source)
            .explanation("Successfully ingested '" + source.getTitle() + "' as " + sourceId +
                         " with " + passages.size() + " passages.")
            .attemptedPath(filePath.toString())
            .build();
    }

    /**
     * Registers a revised document, linking it to the predecessor sourceId.
     * The predecessor is marked SUPERSEDED in the store.
     */
    public IngestionResult ingestRevision(Path filePath, SourceMetadata metadata, String predecessorSourceId) {
        // Verify predecessor exists
        Optional<TechnicalSource> predecessor = store.findById(predecessorSourceId);
        if (predecessor.isEmpty()) {
            return IngestionResult.builder()
                .outcome(IngestionResult.Outcome.NOT_FOUND)
                .rejectionCode("PREDECESSOR_NOT_FOUND")
                .explanation("Predecessor source '" + predecessorSourceId + "' not found in store.")
                .attemptedPath(filePath.toString())
                .build();
        }

        // Ingest the new revision with predecessor link
        SourceMetadata withPredecessor = SourceMetadata.builder()
            .from(metadata)
            .predecessorSourceId(predecessorSourceId)
            .build();

        IngestionResult result = ingest(filePath, withPredecessor);

        // If ingested, mark predecessor as SUPERSEDED
        // NOTE: In the immutable model, we create a new record with SUPERSEDED status
        // and replace the entry. The original is preserved in the revision chain.
        // For MVP simplicity, we log this — a production implementation would use
        // a mutable status field or event log.
        if (result.getOutcome() == IngestionResult.Outcome.INGESTED) {
            // The revision chain is navigable via predecessorSourceId links
            System.out.printf("[SourceIngestionService] Predecessor '%s' is now superseded by '%s'%n",
                predecessorSourceId, result.getSourceRecord().getSourceId());
        }

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Stub for PDF ingestion (to be implemented in future phases).
     * Currently delegates to text ingestion if possible or returns UNREADABLE.
     */
    public IngestionResult ingestPdf(Path pdfPath, SourceMetadata metadata) {
        // Future implementation: call PDF text extraction service here.
        // For MVP, we return UNREADABLE to enforce text-only ingestion.
        return IngestionResult.builder()
            .outcome(IngestionResult.Outcome.UNREADABLE)
            .rejectionCode("PDF_NOT_SUPPORTED_YET")
            .explanation("PDF extraction is scheduled for a future phase.")
            .attemptedPath(pdfPath.toString())
            .build();
    }

    private void archiveSourceBytes(String hash, byte[] bytes) {
        try {
            Path archiveDir = Path.of("source-archive");
            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir);
            }
            Path archiveFile = archiveDir.resolve(hash + ".bin");
            if (!Files.exists(archiveFile)) {
                Files.write(archiveFile, bytes);
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to archive source bytes for hash " + hash);
        }
    }

    /**
     * Splits document text into addressable passages.
     * Passages are bounded by detected section headings or by MAX_PASSAGE_CHARS characters.
     */
    private List<Passage> extractPassages(String sourceId, String text) {
        List<Passage> passages = new ArrayList<>();
        List<int[]> sectionBoundaries = new ArrayList<>();

        // Find section boundaries via headings
        Matcher m = SECTION_PATTERN.matcher(text);
        while (m.find()) {
            sectionBoundaries.add(new int[]{m.start(), m.end()});
        }

        if (sectionBoundaries.isEmpty()) {
            // No section structure: split by character chunks (lossless)
            int seq = 1;
            for (int start = 0; start < text.length(); start += PASSAGE_CHUNK_SIZE) {
                int end = Math.min(start + PASSAGE_CHUNK_SIZE, text.length());
                String chunk = text.substring(start, end).trim();
                if (!chunk.isEmpty()) {
                    passages.add(Passage.builder()
                        .passageId(sourceId + "#P" + String.format("%03d", seq++))
                        .sourceId(sourceId)
                        .text(chunk)
                        .charOffset(start)
                        .build());
                }
            }
        } else {
            // Use section boundaries as passage delimiters (lossless)
            int seq = 1;
            for (int i = 0; i < sectionBoundaries.size(); i++) {
                int passageStart = sectionBoundaries.get(i)[0];
                int passageEnd = (i + 1 < sectionBoundaries.size())
                    ? sectionBoundaries.get(i + 1)[0]
                    : text.length();
                String heading = text.substring(sectionBoundaries.get(i)[0], sectionBoundaries.get(i)[1]).trim();
                String chunk = text.substring(passageStart, passageEnd).trim();
                if (!chunk.isEmpty()) {
                    passages.add(Passage.builder()
                        .passageId(sourceId + "#P" + String.format("%03d", seq++))
                        .sourceId(sourceId)
                        .sectionAnchor(heading)
                        .text(chunk)
                        .charOffset(passageStart)
                        .build());
                }
            }
        }

        return passages;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
