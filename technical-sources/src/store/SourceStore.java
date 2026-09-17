package com.ignitionai.technicalsources.store;

import com.ignitionai.technicalsources.schema.IngestionStatus;
import com.ignitionai.technicalsources.schema.Passage;
import com.ignitionai.technicalsources.schema.TechnicalSource;

import java.util.*;

/**
 * In-memory store for {@link TechnicalSource} records.
 *
 * Responsibilities:
 * - Accept and persist source records (identified by sourceId)
 * - Detect duplicates by document hash
 * - Link revision chains (predecessorSourceId → current sourceId)
 * - Retrieve passages by sourceId and passageId
 *
 * For the MVP, this store operates in-memory with optional JSON serialisation.
 * It does NOT infer or repair missing metadata.
 */
public class SourceStore {

    /** Primary index: sourceId → TechnicalSource */
    private final Map<String, TechnicalSource> bySourceId = new LinkedHashMap<>();

    /** Secondary index: documentHash → sourceId (for duplicate detection) */
    private final Map<String, String> hashIndex = new HashMap<>();

    /**
     * Stores a new source record.
     *
     * @param source The source record to store (must have a unique sourceId).
     * @throws IllegalArgumentException if a source with the same sourceId already exists.
     */
    public synchronized void store(TechnicalSource source) {
        if (bySourceId.containsKey(source.getSourceId())) {
            throw new IllegalArgumentException(
                "Source with sourceId '" + source.getSourceId() + "' already exists. " +
                "Use a distinct sourceId for revisions.");
        }
        bySourceId.put(source.getSourceId(), source);
        hashIndex.put(source.getDocumentHash(), source.getSourceId());
    }

    /**
     * Checks whether a document with the given hash already exists.
     * Returns the existing sourceId if a duplicate is found, otherwise empty.
     */
    public synchronized Optional<String> findByHash(String documentHash) {
        return Optional.ofNullable(hashIndex.get(documentHash));
    }

    /**
     * Retrieves a source record by its sourceId.
     */
    public synchronized Optional<TechnicalSource> findById(String sourceId) {
        return Optional.ofNullable(bySourceId.get(sourceId));
    }

    /**
     * Returns all stored source records.
     */
    public synchronized List<TechnicalSource> findAll() {
        return List.copyOf(bySourceId.values());
    }

    /**
     * Returns all sources with a given ingestion status.
     */
    public synchronized List<TechnicalSource> findByStatus(IngestionStatus status) {
        List<TechnicalSource> result = new ArrayList<>();
        for (TechnicalSource src : bySourceId.values()) {
            if (src.getIngestionStatus() == status) {
                result.add(src);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Retrieves a specific passage by its passageId from its parent source.
     */
    public synchronized Optional<Passage> findPassage(String sourceId, String passageId) {
        TechnicalSource source = bySourceId.get(sourceId);
        if (source == null) return Optional.empty();
        return source.getPassages().stream()
            .filter(p -> p.getPassageId().equals(passageId))
            .findFirst();
    }

    /**
     * Returns the complete revision chain for a given sourceId, ordered oldest → newest.
     * Follows predecessorSourceId links.
     */
    public synchronized List<TechnicalSource> getRevisionChain(String sourceId) {
        List<TechnicalSource> chain = new ArrayList<>();
        // Walk backwards to find the root
        String current = sourceId;
        Set<String> visited = new HashSet<>();
        while (current != null && !visited.contains(current)) {
            TechnicalSource src = bySourceId.get(current);
            if (src == null) break;
            chain.add(0, src); // prepend to maintain oldest-first order
            visited.add(current);
            current = src.getPredecessorSourceId();
        }
        return Collections.unmodifiableList(chain);
    }

    /** Returns the number of stored source records. */
    public synchronized int size() { return bySourceId.size(); }

    /** Clears all records. Intended for testing only. */
    synchronized void clearForTest() {
        bySourceId.clear();
        hashIndex.clear();
    }
}
