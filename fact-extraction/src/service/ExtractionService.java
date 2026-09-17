package com.ignitionai.factextraction.service;

import com.ignitionai.factextraction.extractor.FactExtractor;
import com.ignitionai.factextraction.schema.CandidateFact;
import com.ignitionai.factextraction.schema.ExtractionResult;
import com.ignitionai.factextraction.schema.FactStatus;
import com.ignitionai.technicalsources.schema.Passage;
import com.ignitionai.technicalsources.schema.TechnicalSource;

import java.util.*;

/**
 * Orchestrates fact extraction across all passages of one or more {@link TechnicalSource} records.
 *
 * Maintains a store of all candidate facts produced, keyed by factId.
 * Facts are output of this service; they must pass through ConsistencyCheckService
 * before being eligible for registry publication.
 *
 * This service does NOT:
 * - Grant facts automatic authority (all facts start as CANDIDATE)
 * - Merge competing facts from different sources (competing candidates are preserved)
 * - Use facts directly as runtime rules (that would bypass consistency checks)
 */
public class ExtractionService {

    private final FactExtractor extractor;

    /** Store of all produced candidate facts, keyed by factId. */
    private final Map<String, CandidateFact> factStore = new LinkedHashMap<>();

    public ExtractionService(FactExtractor extractor) {
        this.extractor = Objects.requireNonNull(extractor);
    }

    /**
     * Extracts candidate facts from all passages in a source.
     *
     * @param source The technical source to process.
     * @return A list of extraction results, one per passage.
     */
    public List<ExtractionResult> extractFromSource(TechnicalSource source) {
        Objects.requireNonNull(source, "source is required");

        // Derive vehicle applicability hint from source's vehicle scope (if available)
        String vehicleHint = null;
        if (source.getVehicleScope() != null && source.getVehicleScope().hasDeterminedScope()) {
            vehicleHint = buildVehicleHint(source);
        }

        List<ExtractionResult> results = new ArrayList<>();
        for (Passage passage : source.getPassages()) {
            ExtractionResult result = extractor.extract(passage, vehicleHint);
            results.add(result);

            // Store extracted facts
            for (CandidateFact fact : result.getFacts()) {
                factStore.put(fact.getFactId(), fact);
            }
        }

        System.out.printf("[ExtractionService] Processed %d passages from '%s', extracted %d candidate facts.%n",
            source.getPassages().size(), source.getSourceId(),
            results.stream().mapToInt(ExtractionResult::getFactCount).sum());

        return results;
    }

    /**
     * Returns all candidate facts currently in the store.
     * These are the output of this module, fed into ConsistencyCheckService.
     */
    public List<CandidateFact> getAllCandidateFacts() {
        return List.copyOf(factStore.values());
    }

    /**
     * Returns candidate facts for a specific source.
     */
    public List<CandidateFact> getCandidateFactsForSource(String sourceId) {
        List<CandidateFact> result = new ArrayList<>();
        for (CandidateFact fact : factStore.values()) {
            if (fact.getSourceId().equals(sourceId)) result.add(fact);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Updates the status of a fact (e.g. from CANDIDATE to CHECKED).
     * Called by ConsistencyCheckService after check results are applied.
     */
    public void updateFactStatus(String factId, FactStatus newStatus) {
        CandidateFact existing = factStore.get(factId);
        if (existing == null) throw new IllegalArgumentException("Fact not found: " + factId);
        factStore.put(factId, existing.withStatus(newStatus));
    }

    /**
     * Retrieves a specific fact by its factId.
     */
    public Optional<CandidateFact> findFact(String factId) {
        return Optional.ofNullable(factStore.get(factId));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildVehicleHint(TechnicalSource source) {
        var scope = source.getVehicleScope();
        StringBuilder sb = new StringBuilder();
        if (scope.getManufacturer() != null) sb.append(scope.getManufacturer()).append(" ");
        if (scope.getEngineFamilies() != null && !scope.getEngineFamilies().isEmpty()) {
            sb.append(String.join("/", scope.getEngineFamilies())).append(" ");
        }
        if (scope.getFuelTypes() != null && !scope.getFuelTypes().isEmpty()) {
            sb.append(String.join("/", scope.getFuelTypes()));
        }
        return sb.toString().trim();
    }
}
