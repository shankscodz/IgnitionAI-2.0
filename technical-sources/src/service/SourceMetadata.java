package com.ignitionai.technicalsources.service;

import com.ignitionai.technicalsources.schema.DocumentType;
import com.ignitionai.technicalsources.schema.VehicleScope;

import java.time.LocalDate;

/**
 * Value object carrying caller-supplied metadata for source ingestion.
 *
 * All fields are nullable. The ingestion service must not infer missing fields.
 * If a field is unknown, it must remain null in the stored source record.
 */
public final class SourceMetadata {

    private final String title;
    private final String publisher;
    private final DocumentType documentType;
    private final String originalUrl;
    private final LocalDate publicationDate;
    private final LocalDate revisionDate;
    private final String language;
    private final String accessRestrictions;
    private final VehicleScope vehicleScope;
    private final String predecessorSourceId;
    private final boolean isSynthetic;

    private SourceMetadata(Builder builder) {
        this.title               = builder.title;
        this.publisher           = builder.publisher;
        this.documentType        = builder.documentType;
        this.originalUrl         = builder.originalUrl;
        this.publicationDate     = builder.publicationDate;
        this.revisionDate        = builder.revisionDate;
        this.language            = builder.language;
        this.accessRestrictions  = builder.accessRestrictions;
        this.vehicleScope        = builder.vehicleScope;
        this.predecessorSourceId = builder.predecessorSourceId;
        this.isSynthetic         = builder.isSynthetic;
    }

    public String getTitle()                { return title; }
    public String getPublisher()            { return publisher; }
    public DocumentType getDocumentType()   { return documentType; }
    public String getOriginalUrl()          { return originalUrl; }
    public LocalDate getPublicationDate()   { return publicationDate; }
    public LocalDate getRevisionDate()      { return revisionDate; }
    public String getLanguage()             { return language; }
    public String getAccessRestrictions()   { return accessRestrictions; }
    public VehicleScope getVehicleScope()   { return vehicleScope; }
    public String getPredecessorSourceId()  { return predecessorSourceId; }
    public boolean isSynthetic()            { return isSynthetic; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String title;
        private String publisher;
        private DocumentType documentType;
        private String originalUrl;
        private LocalDate publicationDate;
        private LocalDate revisionDate;
        private String language;
        private String accessRestrictions;
        private VehicleScope vehicleScope;
        private String predecessorSourceId;
        private boolean isSynthetic;

        public Builder title(String title)                           { this.title = title; return this; }
        public Builder publisher(String publisher)                   { this.publisher = publisher; return this; }
        public Builder documentType(DocumentType documentType)       { this.documentType = documentType; return this; }
        public Builder originalUrl(String originalUrl)               { this.originalUrl = originalUrl; return this; }
        public Builder publicationDate(LocalDate publicationDate)    { this.publicationDate = publicationDate; return this; }
        public Builder revisionDate(LocalDate revisionDate)          { this.revisionDate = revisionDate; return this; }
        public Builder language(String language)                     { this.language = language; return this; }
        public Builder accessRestrictions(String restrictions)       { this.accessRestrictions = restrictions; return this; }
        public Builder vehicleScope(VehicleScope vehicleScope)       { this.vehicleScope = vehicleScope; return this; }
        public Builder predecessorSourceId(String predecessorId)     { this.predecessorSourceId = predecessorId; return this; }
        public Builder isSynthetic(boolean isSynthetic)              { this.isSynthetic = isSynthetic; return this; }

        /** Copy all fields from an existing metadata object (for revision ingestion). */
        public Builder from(SourceMetadata other) {
            this.title = other.title; this.publisher = other.publisher;
            this.documentType = other.documentType; this.originalUrl = other.originalUrl;
            this.publicationDate = other.publicationDate; this.revisionDate = other.revisionDate;
            this.language = other.language; this.accessRestrictions = other.accessRestrictions;
            this.vehicleScope = other.vehicleScope; this.predecessorSourceId = other.predecessorSourceId;
            this.isSynthetic = other.isSynthetic;
            return this;
        }

        public SourceMetadata build() { return new SourceMetadata(this); }
    }
}
