package com.ignitionai.technicalsources.schema;

/**
 * Lifecycle status of a {@link TechnicalSource} record.
 *
 * Transitions:
 *   PENDING → INGESTED (successful text extraction and storage)
 *   PENDING → UNREADABLE (file cannot be parsed as text)
 *   PENDING → DUPLICATE (matching document hash already exists)
 *   INGESTED → SUPERSEDED (a newer revision has been ingested and linked)
 */
public enum IngestionStatus {

    /** Document has been registered but not yet fully processed. */
    PENDING,

    /** Document has been successfully ingested; passages are available. */
    INGESTED,

    /** Document could not be read as text. Scans/OCR are a separately estimated extension. */
    UNREADABLE,

    /**
     * A document with the same SHA-256 hash already exists.
     * The duplicate is recorded but not stored again; the existing record is referenced instead.
     */
    DUPLICATE,

    /**
     * A newer revision of this document has been ingested.
     * This record is preserved for provenance; new fact extraction uses the successor.
     */
    SUPERSEDED
}
