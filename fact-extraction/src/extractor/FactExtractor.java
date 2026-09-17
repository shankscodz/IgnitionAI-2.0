package com.ignitionai.factextraction.extractor;

import com.ignitionai.factextraction.schema.*;
import com.ignitionai.technicalsources.schema.Passage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Schema-constrained fact extractor for technical source passages.
 *
 * This is the MVP rule-based extractor. It uses pattern matching to identify
 * structured facts in passage text. An LLM-assisted extractor may be layered
 * on top in a later iteration, but the schema constraints enforced by
 * {@link CandidateFact} apply to all extractor implementations.
 *
 * Extraction rules:
 * 1. Absent information stays absent — fields not found in the passage remain null.
 * 2. LLM assistance may be used, but absent information must remain absent;
 *    require source support rather than plausible completion.
 * 3. extractionConfidence is separate from sourceAuthority.
 * 4. Multiple facts may be produced from a single passage.
 * 5. The extractor version and settings are stored on every fact for reproducibility.
 */
public class FactExtractor {

    private static final String EXTRACTOR_VERSION = "rule-based-v1.0";
    private static final String EXTRACTION_SETTINGS = "pattern-matching; no LLM; absent-stays-absent";

    // Pattern: "X must not exceed N unit" or "X should be between N1 and N2 unit"
    private static final Pattern THRESHOLD_PATTERN = Pattern.compile(
        "(?i)(\\w[\\w\\s/]+?)\\s+(must not exceed|should not exceed|shall not exceed)\\s+" +
        "([0-9]+(?:\\.[0-9]+)?)\\s*([°%a-zA-Z]+(?:\\s+[a-zA-Z]+)?)(?:\\s+(at|during|for|if|when|after|under)\\s+([^.]+?))?(?=\\.|$)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "(?i)(\\w[\\w\\s/]+?)\\s+(must be|should be|shall be)\\s+(?:between\\s+)?" +
        "([0-9]+(?:\\.[0-9]+)?)\\s*(?:and|–|-)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([°%a-zA-Z]+(?:\\s+[a-zA-Z]+)?)(?:\\s+(at|during|for|if|when|after|under)\\s+([^.]+?))?(?=\\.|$)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern SYMPTOM_FAULT_PATTERN = Pattern.compile(
        "(?i)((?:(?:white|blue|black|grey)\\s+smoke|(?:rough|erratic)\\s+idle|(?:knocking|pinging|misfir\\w*))[^.]*)" +
        "\\s+(?:may indicate|indicates?|suggests?|is consistent with)\\s+(.+?)(?:\\.|$)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "(?i)(do\\s+not\\s+|never\\s+)?(replace|inspect|check|clean|adjust|reset|perform|calibrate)\\s+(?:the\\s+)?" +
        "([a-zA-Z][\\w\\s/,-]{3,60}?)(?:\\s+(if|when|after|under)\\s+([^.]+?))?(?=\\.|$)",
        Pattern.CASE_INSENSITIVE);

    private final AtomicInteger factSequence = new AtomicInteger(1);

    /**
     * Extracts candidate facts from a single passage.
     *
     * @param passage   The passage to extract facts from.
     * @param vehicleApplicabilityHint An optional hint about the vehicle applicability from
     *                                 the source's VehicleScope. The extractor may use this as
     *                                 context but must not invent applicability if the passage
     *                                 does not support it.
     * @return An ExtractionResult with zero or more candidate facts.
     */
    public ExtractionResult extract(Passage passage, String vehicleApplicabilityHint) {
        Objects.requireNonNull(passage, "passage is required");

        String text = passage.getText();
        if (text == null || text.isBlank() || text.length() < 20) {
            return ExtractionResult.builder()
                .outcome(ExtractionResult.Outcome.INSUFFICIENT_CONTENT)
                .passageId(passage.getPassageId())
                .sourceId(passage.getSourceId())
                .explanation("Passage text is too short or blank to extract meaningful facts.")
                .build();
        }

        List<CandidateFact> facts = new ArrayList<>();

        // Extract threshold facts (single limit: "must not exceed N unit")
        facts.addAll(extractThresholdFacts(passage, text, vehicleApplicabilityHint));

        // Extract range facts ("should be between N1 and N2 unit")
        facts.addAll(extractRangeFacts(passage, text, vehicleApplicabilityHint));

        // Extract symptom-fault relationships
        facts.addAll(extractSymptomFaultFacts(passage, text, vehicleApplicabilityHint));

        // Extract action facts
        facts.addAll(extractActionFacts(passage, text, vehicleApplicabilityHint));

        if (facts.isEmpty()) {
            return ExtractionResult.builder()
                .outcome(ExtractionResult.Outcome.NO_FACTS_FOUND)
                .passageId(passage.getPassageId())
                .sourceId(passage.getSourceId())
                .explanation("No extractable structured facts found in this passage.")
                .build();
        }

        return ExtractionResult.builder()
            .outcome(ExtractionResult.Outcome.FACTS_EXTRACTED)
            .passageId(passage.getPassageId())
            .sourceId(passage.getSourceId())
            .facts(facts)
            .explanation("Extracted " + facts.size() + " candidate fact(s) from passage " + passage.getPassageId() + ".")
            .build();
    }

    // ── Private extraction helpers ─────────────────────────────────────────────

    private List<CandidateFact> extractThresholdFacts(Passage passage, String text, String vehicleHint) {
        List<CandidateFact> facts = new ArrayList<>();
        Matcher m = THRESHOLD_PATTERN.matcher(text);
        while (m.find()) {
            String quantity = m.group(1).trim();
            String operator = m.group(2).trim();
            double value;
            try { value = Double.parseDouble(m.group(3)); }
            catch (NumberFormatException e) { continue; }
            String unit = m.group(4).trim();
            String condition = m.group(5) != null ? m.group(5).trim() : null;

            // Only extract if we have all three required parts: quantity, value, unit
            if (quantity.isBlank() || unit.isBlank()) continue;
            
            String quantityId = quantity.toUpperCase().replaceAll("[^A-Z0-9]+", "_");

            facts.add(CandidateFact.builder()
                .factId(nextFactId())
                .factType(FactType.THRESHOLD)
                .sourceId(passage.getSourceId())
                .passageId(passage.getPassageId())
                .supportingPassageText(passage.getText())
                .sourceSpan(m.group(0))
                .vehicleApplicability(vehicleHint)     // may be null — not inferred
                .quantityId(quantityId)
                .originalQuantityText(quantity)
                .comparisonOperator("<=")
                .thresholdRole("ABSOLUTE_LIMIT") // default heuristic for 'must not exceed'
                .conditionExpression(condition)
                .thresholdUpper(value)                 // "must not exceed" = upper limit
                .unit(unit)
                .extractorVersion(EXTRACTOR_VERSION)
                .extractionSettings(EXTRACTION_SETTINGS)
                .extractionConfidence(0.75)            // rule-based confidence
                .build());
        }
        return facts;
    }

    private List<CandidateFact> extractRangeFacts(Passage passage, String text, String vehicleHint) {
        List<CandidateFact> facts = new ArrayList<>();
        Matcher m = RANGE_PATTERN.matcher(text);
        while (m.find()) {
            String quantity = m.group(1).trim();
            String operator = m.group(2).trim();
            double lower, upper;
            try {
                lower = Double.parseDouble(m.group(3));
                upper = Double.parseDouble(m.group(4));
            } catch (NumberFormatException e) { continue; }
            String unit = m.group(5).trim();
            String condition = m.group(6) != null ? m.group(6).trim() : null;

            if (quantity.isBlank() || unit.isBlank()) continue;
            
            String quantityId = quantity.toUpperCase().replaceAll("[^A-Z0-9]+", "_");

            facts.add(CandidateFact.builder()
                .factId(nextFactId())
                .factType(FactType.THRESHOLD)
                .sourceId(passage.getSourceId())
                .passageId(passage.getPassageId())
                .supportingPassageText(passage.getText())
                .sourceSpan(m.group(0))
                .vehicleApplicability(vehicleHint)
                .quantityId(quantityId)
                .originalQuantityText(quantity)
                .comparisonOperator("BETWEEN")
                .thresholdRole("NORMAL_OPERATING")
                .conditionExpression(condition)
                .thresholdLower(lower)
                .thresholdUpper(upper)
                .unit(unit)
                .extractorVersion(EXTRACTOR_VERSION)
                .extractionSettings(EXTRACTION_SETTINGS)
                .extractionConfidence(0.80)
                .build());
        }
        return facts;
    }

    private List<CandidateFact> extractSymptomFaultFacts(Passage passage, String text, String vehicleHint) {
        List<CandidateFact> facts = new ArrayList<>();
        Matcher m = SYMPTOM_FAULT_PATTERN.matcher(text);
        while (m.find()) {
            String symptom = m.group(1).trim();
            String fault   = m.group(2).trim();
            if (symptom.isBlank() || fault.isBlank()) continue;

            facts.add(CandidateFact.builder()
                .factId(nextFactId())
                .factType(FactType.SYMPTOM_FAULT)
                .sourceId(passage.getSourceId())
                .passageId(passage.getPassageId())
                .supportingPassageText(passage.getText())
                .vehicleApplicability(vehicleHint)
                .symptomDescription(symptom)
                .faultDescription(fault)
                .extractorVersion(EXTRACTOR_VERSION)
                .extractionSettings(EXTRACTION_SETTINGS)
                .extractionConfidence(0.65)
                .build());
        }
        return facts;
    }

    private List<CandidateFact> extractActionFacts(Passage passage, String text, String vehicleHint) {
        List<CandidateFact> facts = new ArrayList<>();
        Matcher m = ACTION_PATTERN.matcher(text);
        while (m.find()) {
            String negation = m.group(1);
            String verb = m.group(2);
            String object = m.group(3);
            String action = verb + " " + object;
            String conditionWord = m.group(4);
            String conditionBody = m.group(5);
            
            if (action.length() < 5) continue; // skip very short matches

            String polarity = (negation != null && !negation.isBlank()) ? "NEGATIVE" : "AFFIRMATIVE";
            List<String> preconditions = new ArrayList<>();
            if (conditionBody != null && !conditionBody.isBlank()) {
                preconditions.add(conditionWord + " " + conditionBody.trim());
            }

            facts.add(CandidateFact.builder()
                .factId(nextFactId())
                .factType(FactType.ACTION)
                .sourceId(passage.getSourceId())
                .passageId(passage.getPassageId())
                .supportingPassageText(passage.getText())
                .sourceSpan(m.group(0))
                .vehicleApplicability(vehicleHint)
                .actionDescription(action)
                .actionPolarity(polarity)
                .preconditions(preconditions)
                .extractorVersion(EXTRACTOR_VERSION)
                .extractionSettings(EXTRACTION_SETTINGS)
                .extractionConfidence(0.60)
                .build());
        }
        return facts;
    }

    private String nextFactId() {
        return "FACT-" + String.format("%06d", factSequence.getAndIncrement());
    }
}
