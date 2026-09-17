package com.ignitionai.factextraction.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A candidate structured fact extracted from a technical source passage.
 *
 * Candidate facts feed into Consistency Checks and are never used directly
 * as runtime rules. The pipeline enforces:
 *   CandidateFact → ConsistencyCheckService → (if REVIEWED) → LinkedRegistries
 *
 * Design rules enforced by this class:
 * - Absent information stays absent. Fields not found in the source passage must remain null.
 * - extractionConfidence is kept separate from sourceAuthority (different concepts).
 * - A THRESHOLD fact must carry unit, operatingConditions and vehicleApplicability.
 *   An isolated number without meaning is rejected at construction.
 * - Multiple candidates may reference the same passageId (competing extractions).
 * - The extractorModel and extractionVersion are stored for reproducibility.
 */
public final class CandidateFact {

    /** Stable unique fact identifier assigned at extraction time. */
    private final String factId;

    /** The type of knowledge captured. Determines required structured fields. */
    private final FactType factType;

    /** The sourceId of the document this fact was extracted from. */
    private final String sourceId;

    /** The passageId of the specific passage that supports this fact. */
    private final String passageId;

    /** Verbatim copy of the supporting passage text at extraction time. */
    private final String supportingPassageText;

    /**
     * Vehicle applicability of this fact. Null means applicability is undetermined
     * (not that the fact applies to all vehicles).
     */
    private final String vehicleApplicability;

    /**
     * Operating conditions under which this fact holds.
     * Example: "engine at operating temperature, idle regime".
     * Null if not specified in the source passage.
     */
    private final String operatingConditions;

    /**
     * Structured inputs to the fact (e.g. sensor signals consumed by a diagnostic action).
     * Key: input name. Value: unit or description. Null keys not permitted.
     */
    private final Map<String, String> inputs;

    /**
     * Structured outputs of the fact (e.g. expected observations, diagnostic conclusions).
     */
    private final Map<String, String> outputs;

    /** [F3] Precise span within the passage that supports the fact. */
    private final String sourceSpan;

    // ── Type-specific fields (populated only for the relevant FactType) ────────

    /** [F3] Canonical identifier for the measured quantity (e.g. "COOLANT_TEMP"). */
    private final String quantityId;

    /** [F3] Original text describing the quantity. */
    private final String originalQuantityText;

    /** [F3] Comparison operator (e.g. "<=", ">", "BETWEEN"). */
    private final String comparisonOperator;

    /** [F3] Role of the threshold (e.g. "NORMAL_OPERATING", "ABSOLUTE_LIMIT"). */
    private final String thresholdRole;

    /** [F3] Condition expression (e.g. "engine at warm idle"). */
    private final String conditionExpression;

    /** [F3] Duration over which the condition must hold (e.g. "5 seconds"). */
    private final String duration;

    /** [THRESHOLD] The numeric value or range lower bound. Null for non-threshold facts. */
    private final Double thresholdLower;

    /** [THRESHOLD] The numeric upper bound. Null if only a single value or not applicable. */
    private final Double thresholdUpper;

    /** [THRESHOLD/EXPECTED_OBSERVATION] Physical unit (e.g. "°C", "bar", "%"). */
    private final String unit;

    /** [SYMPTOM_FAULT] The symptom description. */
    private final String symptomDescription;

    /** [SYMPTOM_FAULT] The possible fault description. */
    private final String faultDescription;

    /** [ACTION/TOOL_REQUIREMENT] Action or tool description. */
    private final String actionDescription;

    /** [F2] Polarity of the action (e.g., "AFFIRMATIVE", "NEGATIVE"). */
    private final String actionPolarity;

    /** [F2] Preconditions required for the action (from "if", "when", etc.). */
    private final List<String> preconditions;

    /** [ACTION] Estimated time (e.g. "0.5h") from source; null if not stated. */
    private final String estimatedTime;

    // ── Extraction provenance ─────────────────────────────────────────────────

    /**
     * Version string of the extraction algorithm or LLM model used.
     * Stored for reproducibility. Required — an isolated extraction cannot be reproduced
     * without knowing what produced it.
     */
    private final String extractorVersion;

    /**
     * Extraction settings snapshot (e.g. prompt version, temperature setting).
     * Stored for reproducibility.
     */
    private final String extractionSettings;

    /**
     * Confidence score of the extractor in this fact [0.0–1.0].
     * This is extractor confidence, NOT source authority.
     * A low-confidence extraction from an authoritative source remains low-confidence.
     * A high-confidence extraction from a dubious source is still low-authority.
     */
    private final Double extractionConfidence;

    /** Current status in the knowledge pipeline lifecycle. Starts as CANDIDATE. */
    private final FactStatus status;

    private CandidateFact(Builder builder) {
        this.factId               = Objects.requireNonNull(builder.factId, "factId is required");
        this.factType             = Objects.requireNonNull(builder.factType, "factType is required");
        this.sourceId             = Objects.requireNonNull(builder.sourceId, "sourceId is required");
        this.passageId            = Objects.requireNonNull(builder.passageId, "passageId is required");
        this.supportingPassageText = Objects.requireNonNull(builder.supportingPassageText, "supportingPassageText is required");
        this.extractorVersion     = Objects.requireNonNull(builder.extractorVersion, "extractorVersion is required");

        // Absent information stays absent — never inferred
        this.vehicleApplicability = builder.vehicleApplicability;
        this.operatingConditions  = builder.operatingConditions;
        this.inputs               = builder.inputs != null ? Map.copyOf(builder.inputs) : null;
        this.outputs              = builder.outputs != null ? Map.copyOf(builder.outputs) : null;
        this.sourceSpan           = builder.sourceSpan;
        
        this.quantityId           = builder.quantityId;
        this.originalQuantityText = builder.originalQuantityText;
        this.comparisonOperator   = builder.comparisonOperator;
        this.thresholdRole        = builder.thresholdRole;
        this.conditionExpression  = builder.conditionExpression;
        this.duration             = builder.duration;
        this.thresholdLower       = builder.thresholdLower;
        this.thresholdUpper       = builder.thresholdUpper;
        this.unit                 = builder.unit;
        
        this.symptomDescription   = builder.symptomDescription;
        this.faultDescription     = builder.faultDescription;
        
        this.actionDescription    = builder.actionDescription;
        this.actionPolarity       = builder.actionPolarity;
        this.preconditions        = builder.preconditions != null ? List.copyOf(builder.preconditions) : null;
        this.estimatedTime        = builder.estimatedTime;
        
        this.extractionSettings   = builder.extractionSettings;
        this.extractionConfidence = builder.extractionConfidence;
        this.status               = builder.status != null ? builder.status : FactStatus.CANDIDATE;

        // Enforce threshold completeness: a threshold without unit is physically meaningless
        if (factType == FactType.THRESHOLD) {
            if ((thresholdLower == null && thresholdUpper == null)) {
                throw new IllegalArgumentException(
                    "THRESHOLD fact '" + factId + "' must have at least one of thresholdLower or thresholdUpper.");
            }
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException(
                    "THRESHOLD fact '" + factId + "' must have a unit. " +
                    "An isolated number without a unit has no physical meaning.");
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getFactId()                { return factId; }
    public FactType getFactType()            { return factType; }
    public String getSourceId()              { return sourceId; }
    public String getPassageId()             { return passageId; }
    public String getSupportingPassageText() { return supportingPassageText; }
    public String getVehicleApplicability()  { return vehicleApplicability; }
    public String getOperatingConditions()   { return operatingConditions; }
    public Map<String, String> getInputs()   { return inputs; }
    public Map<String, String> getOutputs()  { return outputs; }
    public String getSourceSpan()            { return sourceSpan; }
    
    public String getQuantityId()            { return quantityId; }
    public String getOriginalQuantityText()  { return originalQuantityText; }
    public String getComparisonOperator()    { return comparisonOperator; }
    public String getThresholdRole()         { return thresholdRole; }
    public String getConditionExpression()   { return conditionExpression; }
    public String getDuration()              { return duration; }
    
    public Double getThresholdLower()        { return thresholdLower; }
    public Double getThresholdUpper()        { return thresholdUpper; }
    public String getUnit()                  { return unit; }
    
    public String getSymptomDescription()    { return symptomDescription; }
    public String getFaultDescription()      { return faultDescription; }
    
    public String getActionDescription()     { return actionDescription; }
    public String getActionPolarity()        { return actionPolarity; }
    public List<String> getPreconditions()   { return preconditions; }
    public String getEstimatedTime()         { return estimatedTime; }
    public String getExtractorVersion()      { return extractorVersion; }
    public String getExtractionSettings()    { return extractionSettings; }
    public Double getExtractionConfidence()  { return extractionConfidence; }
    public FactStatus getStatus()            { return status; }

    /** Returns a copy of this fact with the status updated. */
    public CandidateFact withStatus(FactStatus newStatus) {
        return builder().from(this).status(newStatus).build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String factId;
        private FactType factType;
        private String sourceId;
        private String passageId;
        private String supportingPassageText;
        private String vehicleApplicability;
        private String operatingConditions;
        private Map<String, String> inputs;
        private Map<String, String> outputs;
        private String sourceSpan;
        
        private String quantityId;
        private String originalQuantityText;
        private String comparisonOperator;
        private String thresholdRole;
        private String conditionExpression;
        private String duration;
        
        private Double thresholdLower;
        private Double thresholdUpper;
        private String unit;
        
        private String symptomDescription;
        private String faultDescription;
        
        private String actionDescription;
        private String actionPolarity;
        private List<String> preconditions;
        private String estimatedTime;
        
        private String extractorVersion;
        private String extractionSettings;
        private Double extractionConfidence;
        private FactStatus status;

        public Builder factId(String v)                { this.factId = v; return this; }
        public Builder factType(FactType v)            { this.factType = v; return this; }
        public Builder sourceId(String v)              { this.sourceId = v; return this; }
        public Builder passageId(String v)             { this.passageId = v; return this; }
        public Builder supportingPassageText(String v) { this.supportingPassageText = v; return this; }
        public Builder vehicleApplicability(String v)  { this.vehicleApplicability = v; return this; }
        public Builder operatingConditions(String v)   { this.operatingConditions = v; return this; }
        public Builder inputs(Map<String, String> v)   { this.inputs = v; return this; }
        public Builder outputs(Map<String, String> v)  { this.outputs = v; return this; }
        public Builder sourceSpan(String v)            { this.sourceSpan = v; return this; }
        
        public Builder quantityId(String v)            { this.quantityId = v; return this; }
        public Builder originalQuantityText(String v)  { this.originalQuantityText = v; return this; }
        public Builder comparisonOperator(String v)    { this.comparisonOperator = v; return this; }
        public Builder thresholdRole(String v)         { this.thresholdRole = v; return this; }
        public Builder conditionExpression(String v)   { this.conditionExpression = v; return this; }
        public Builder duration(String v)              { this.duration = v; return this; }
        
        public Builder thresholdLower(Double v)        { this.thresholdLower = v; return this; }
        public Builder thresholdUpper(Double v)        { this.thresholdUpper = v; return this; }
        public Builder unit(String v)                  { this.unit = v; return this; }
        
        public Builder symptomDescription(String v)    { this.symptomDescription = v; return this; }
        public Builder faultDescription(String v)      { this.faultDescription = v; return this; }
        
        public Builder actionDescription(String v)     { this.actionDescription = v; return this; }
        public Builder actionPolarity(String v)        { this.actionPolarity = v; return this; }
        public Builder preconditions(List<String> v)   { this.preconditions = v; return this; }
        public Builder estimatedTime(String v)         { this.estimatedTime = v; return this; }
        
        public Builder extractorVersion(String v)      { this.extractorVersion = v; return this; }
        public Builder extractionSettings(String v)    { this.extractionSettings = v; return this; }
        public Builder extractionConfidence(Double v)  { this.extractionConfidence = v; return this; }
        public Builder status(FactStatus v)            { this.status = v; return this; }

        public Builder from(CandidateFact f) {
            this.factId = f.factId; this.factType = f.factType; this.sourceId = f.sourceId;
            this.passageId = f.passageId; this.supportingPassageText = f.supportingPassageText;
            this.vehicleApplicability = f.vehicleApplicability; this.operatingConditions = f.operatingConditions;
            this.inputs = f.inputs; this.outputs = f.outputs; this.sourceSpan = f.sourceSpan;
            
            this.quantityId = f.quantityId; this.originalQuantityText = f.originalQuantityText;
            this.comparisonOperator = f.comparisonOperator; this.thresholdRole = f.thresholdRole;
            this.conditionExpression = f.conditionExpression; this.duration = f.duration;
            this.thresholdLower = f.thresholdLower; this.thresholdUpper = f.thresholdUpper; this.unit = f.unit;
            
            this.symptomDescription = f.symptomDescription; this.faultDescription = f.faultDescription;
            
            this.actionDescription = f.actionDescription; this.actionPolarity = f.actionPolarity;
            this.preconditions = f.preconditions; this.estimatedTime = f.estimatedTime;
            
            this.extractorVersion = f.extractorVersion; this.extractionSettings = f.extractionSettings;
            this.extractionConfidence = f.extractionConfidence; this.status = f.status;
            return this;
        }

        public CandidateFact build() { return new CandidateFact(this); }
    }

    @Override
    public String toString() {
        return "CandidateFact{factId='" + factId + "', type=" + factType + ", source=" + sourceId +
               ", passage=" + passageId + ", status=" + status + "}";
    }
}
