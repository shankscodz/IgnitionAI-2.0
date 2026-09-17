package com.ignitionai.technicalsources.schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, identifiable record of a technical document.
 *
 * Every extracted fact must reference back to a {@code TechnicalSource} and a specific
 * {@link Passage} within it. The source retains its original document plus revision chain
 * so provenance can always be verified.
 *
 * Required fields: sourceId, title, documentType, retrievalDate, documentHash, ingestionStatus.
 * All other fields use explicit "unknown" / null values — they are never inferred.
 */
public final class TechnicalSource {

    /** Stable, globally unique identifier assigned at ingestion time. */
    private final String sourceId;

    /** Human-readable document title. */
    private final String title;

    /** Publisher or issuing organisation (null if unknown, never inferred). */
    private final String publisher;

    /** Type of document: SERVICE_MANUAL, TECHNICAL_BULLETIN, WIRING_DIAGRAM, etc. */
    private final DocumentType documentType;

    /** Original URL or file location at the time of retrieval. */
    private final String originalUrl;

    /** Local file path of the preserved copy (null if not yet stored). */
    private final String fileLocation;

    /** Date the document was originally published by the publisher. Null if unknown. */
    private final LocalDate publicationDate;

    /** Date of the document's revision as stated by the publisher. Null if unknown. */
    private final LocalDate revisionDate;

    /** Date this document was retrieved and ingested. Never null. */
    private final LocalDate retrievalDate;

    /** ISO 639-1 language code, e.g. "en". Null if unknown. */
    private final String language;

    /**
     * SHA-256 hex digest of the original document bytes.
     * Used for duplicate detection and integrity verification.
     */
    private final String documentHash;

    /** Access and redistribution restrictions. Null means restrictions are unknown, not absent. */
    private final String accessRestrictions;

    /**
     * Vehicle scope this document applies to.
     * Expressed as a structured applicability descriptor; null if the scope is
     * not determined (must not be assumed universal).
     */
    private final VehicleScope vehicleScope;

    /** Current ingestion/lifecycle status of this source record. Never null. */
    private final IngestionStatus ingestionStatus;

    /**
     * If this source supersedes an older version, this field holds the sourceId of the
     * predecessor. Null for first-revision documents.
     */
    private final String predecessorSourceId;

    /** Indicates if this source is synthetic data (demo/test) or a real OEM document. */
    private final boolean isSynthetic;

    /**
     * Addressable text passages extracted from this document.
     * Each passage carries a page/section anchor so facts can cite the exact text.
     */
    private final List<Passage> passages;

    private TechnicalSource(Builder builder) {
        this.sourceId          = Objects.requireNonNull(builder.sourceId, "sourceId is required");
        this.title             = Objects.requireNonNull(builder.title, "title is required");
        this.publisher         = builder.publisher;
        this.documentType      = Objects.requireNonNull(builder.documentType, "documentType is required");
        this.originalUrl       = builder.originalUrl;
        this.fileLocation      = builder.fileLocation;
        this.publicationDate   = builder.publicationDate;
        this.revisionDate      = builder.revisionDate;
        this.retrievalDate     = Objects.requireNonNull(builder.retrievalDate, "retrievalDate is required");
        this.language          = builder.language;
        this.documentHash      = Objects.requireNonNull(builder.documentHash, "documentHash is required");
        this.accessRestrictions = builder.accessRestrictions;
        this.vehicleScope      = builder.vehicleScope;
        this.ingestionStatus   = Objects.requireNonNull(builder.ingestionStatus, "ingestionStatus is required");
        this.predecessorSourceId = builder.predecessorSourceId;
        this.isSynthetic       = builder.isSynthetic;
        this.passages          = builder.passages != null ? List.copyOf(builder.passages) : List.of();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getSourceId()              { return sourceId; }
    public String getTitle()                 { return title; }
    public String getPublisher()             { return publisher; }
    public DocumentType getDocumentType()    { return documentType; }
    public String getOriginalUrl()           { return originalUrl; }
    public String getFileLocation()          { return fileLocation; }
    public LocalDate getPublicationDate()    { return publicationDate; }
    public LocalDate getRevisionDate()       { return revisionDate; }
    public LocalDate getRetrievalDate()      { return retrievalDate; }
    public String getLanguage()              { return language; }
    public String getDocumentHash()          { return documentHash; }
    public String getAccessRestrictions()    { return accessRestrictions; }
    public VehicleScope getVehicleScope()    { return vehicleScope; }
    public IngestionStatus getIngestionStatus() { return ingestionStatus; }
    public String getPredecessorSourceId()   { return predecessorSourceId; }
    public boolean isSynthetic()             { return isSynthetic; }
    public List<Passage> getPassages()       { return passages; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String sourceId;
        private String title;
        private String publisher;
        private DocumentType documentType;
        private String originalUrl;
        private String fileLocation;
        private LocalDate publicationDate;
        private LocalDate revisionDate;
        private LocalDate retrievalDate;
        private String language;
        private String documentHash;
        private String accessRestrictions;
        private VehicleScope vehicleScope;
        private IngestionStatus ingestionStatus;
        private String predecessorSourceId;
        private boolean isSynthetic;
        private List<Passage> passages;

        public Builder sourceId(String sourceId)                         { this.sourceId = sourceId; return this; }
        public Builder title(String title)                               { this.title = title; return this; }
        public Builder publisher(String publisher)                       { this.publisher = publisher; return this; }
        public Builder documentType(DocumentType documentType)           { this.documentType = documentType; return this; }
        public Builder originalUrl(String originalUrl)                   { this.originalUrl = originalUrl; return this; }
        public Builder fileLocation(String fileLocation)                 { this.fileLocation = fileLocation; return this; }
        public Builder publicationDate(LocalDate publicationDate)        { this.publicationDate = publicationDate; return this; }
        public Builder revisionDate(LocalDate revisionDate)              { this.revisionDate = revisionDate; return this; }
        public Builder retrievalDate(LocalDate retrievalDate)            { this.retrievalDate = retrievalDate; return this; }
        public Builder language(String language)                         { this.language = language; return this; }
        public Builder documentHash(String documentHash)                 { this.documentHash = documentHash; return this; }
        public Builder accessRestrictions(String accessRestrictions)     { this.accessRestrictions = accessRestrictions; return this; }
        public Builder vehicleScope(VehicleScope vehicleScope)           { this.vehicleScope = vehicleScope; return this; }
        public Builder ingestionStatus(IngestionStatus ingestionStatus)  { this.ingestionStatus = ingestionStatus; return this; }
        public Builder predecessorSourceId(String predecessorSourceId)   { this.predecessorSourceId = predecessorSourceId; return this; }
        public Builder isSynthetic(boolean isSynthetic)                  { this.isSynthetic = isSynthetic; return this; }
        public Builder passages(List<Passage> passages)                  { this.passages = passages; return this; }

        public TechnicalSource build() { return new TechnicalSource(this); }
    }

    @Override
    public String toString() {
        return "TechnicalSource{sourceId='" + sourceId + "', title='" + title +
               "', status=" + ingestionStatus + ", hash=" + documentHash.substring(0, 8) + "...}";
    }
}
