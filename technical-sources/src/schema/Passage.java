package com.ignitionai.technicalsources.schema;

import java.util.Objects;

/**
 * An addressable text passage extracted from a {@link TechnicalSource}.
 *
 * Every {@link com.ignitionai.factextraction.schema.CandidateFact} must cite the
 * {@code passageId} of the supporting passage so provenance can always be verified
 * by retrieving the original text.
 */
public final class Passage {

    /** Unique passage identifier within the parent source. Format: "{sourceId}#P{sequence}". */
    private final String passageId;

    /** The source this passage belongs to. */
    private final String sourceId;

    /**
     * Page number within the document. Null for electronic formats without page concepts.
     * Never inferred — if unknown, remains null.
     */
    private final Integer pageNumber;

    /**
     * Section heading or anchor (e.g. "Section 3.2.1 — Coolant System Checks").
     * Null if no section structure is available.
     */
    private final String sectionAnchor;

    /** The verbatim text of the passage as extracted from the source document. */
    private final String text;

    /**
     * Character offset of this passage within the document's full extracted text, if available.
     * Used for precise re-location during provenance verification.
     */
    private final Integer charOffset;

    private Passage(Builder builder) {
        this.passageId     = Objects.requireNonNull(builder.passageId, "passageId is required");
        this.sourceId      = Objects.requireNonNull(builder.sourceId, "sourceId is required");
        this.text          = Objects.requireNonNull(builder.text, "text is required");
        this.pageNumber    = builder.pageNumber;
        this.sectionAnchor = builder.sectionAnchor;
        this.charOffset    = builder.charOffset;
    }

    public String getPassageId()     { return passageId; }
    public String getSourceId()      { return sourceId; }
    public Integer getPageNumber()   { return pageNumber; }
    public String getSectionAnchor() { return sectionAnchor; }
    public String getText()          { return text; }
    public Integer getCharOffset()   { return charOffset; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String passageId;
        private String sourceId;
        private Integer pageNumber;
        private String sectionAnchor;
        private String text;
        private Integer charOffset;

        public Builder passageId(String passageId)         { this.passageId = passageId; return this; }
        public Builder sourceId(String sourceId)           { this.sourceId = sourceId; return this; }
        public Builder pageNumber(Integer pageNumber)       { this.pageNumber = pageNumber; return this; }
        public Builder sectionAnchor(String sectionAnchor) { this.sectionAnchor = sectionAnchor; return this; }
        public Builder text(String text)                   { this.text = text; return this; }
        public Builder charOffset(Integer charOffset)      { this.charOffset = charOffset; return this; }

        public Passage build() { return new Passage(this); }
    }

    @Override
    public String toString() {
        return "Passage{passageId='" + passageId + "', page=" + pageNumber +
               ", section='" + sectionAnchor + "', textLen=" + text.length() + "}";
    }
}
