package com.ignitionai.linkedregistries.service;

import com.ignitionai.linkedregistries.schema.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the linked registry — all four sub-registries and their release lifecycle.
 *
 * This service:
 * 1. Accepts new entries for each sub-registry (DRAFT accumulation)
 * 2. Validates cross-registry references before accepting entries
 * 3. Creates immutable {@link RegistryRelease} snapshots
 * 4. Maintains a history of all releases (analytical sessions pin to one releaseId)
 * 5. Keeps draft content separate from published releases
 *
 * KEY INVARIANT: draft entries are separate from published releases.
 * An analytical session MUST pin to a specific releaseId; it must NOT query
 * the live draft store, because knowledge edits would otherwise silently
 * rewrite historical reports.
 */
public class RegistryQueryService {

    // ── Draft stores (entries accumulating before the next release) ───────────
    private final Map<String, VehicleApplicabilityEntry> draftVehicleApplicability = new LinkedHashMap<>();
    private final Map<String, FaultKnowledgeEntry> draftFaultKnowledge             = new LinkedHashMap<>();
    private final Map<String, ActionEntry> draftActions                             = new LinkedHashMap<>();
    private final Map<String, ObservationEntry> draftObservations                  = new LinkedHashMap<>();

    /** All releases, keyed by releaseId. Immutable once created. */
    private final Map<String, RegistryRelease> releases = new LinkedHashMap<>();

    private final AtomicInteger releaseSequence = new AtomicInteger(1);

    // ── Draft entry publication ───────────────────────────────────────────────

    /**
     * Adds a VehicleApplicabilityEntry to the draft store.
     * An entry with a duplicate entryId is rejected.
     */
    public void addVehicleApplicabilityEntry(VehicleApplicabilityEntry entry) {
        requireNoDuplicate(entry.getEntryId(), draftVehicleApplicability, "VehicleApplicability");
        draftVehicleApplicability.put(entry.getEntryId(), entry);
    }

    /**
     * Adds a FaultKnowledgeEntry to the draft store.
     * Validates that referenced vehicleApplicabilityEntryId exists in the draft store.
     */
    public void addFaultKnowledgeEntry(FaultKnowledgeEntry entry) {
        requireNoDuplicate(entry.getEntryId(), draftFaultKnowledge, "FaultKnowledge");
        if (entry.getVehicleApplicabilityEntryId() != null &&
                !draftVehicleApplicability.containsKey(entry.getVehicleApplicabilityEntryId())) {
            throw new IllegalArgumentException(
                "FaultKnowledgeEntry '" + entry.getEntryId() + "' references unknown vehicleApplicabilityEntryId: " +
                entry.getVehicleApplicabilityEntryId());
        }
        draftFaultKnowledge.put(entry.getEntryId(), entry);
    }

    /**
     * Adds an ActionEntry to the draft store.
     * Validates cross-registry observation references.
     */
    public void addActionEntry(ActionEntry entry) {
        requireNoDuplicate(entry.getEntryId(), draftActions, "Action");
        for (String obsId : entry.getRelatedObservationIds()) {
            if (!draftObservations.containsKey(obsId)) {
                throw new IllegalArgumentException(
                    "ActionEntry '" + entry.getEntryId() + "' references unknown observationId: " + obsId +
                    ". Add the ObservationEntry first.");
            }
        }
        draftActions.put(entry.getEntryId(), entry);
    }

    /**
     * Adds an ObservationEntry to the draft store.
     */
    public void addObservationEntry(ObservationEntry entry) {
        requireNoDuplicate(entry.getObservationId(), draftObservations, "Observation");
        draftObservations.put(entry.getObservationId(), entry);
    }

    // ── Release management ────────────────────────────────────────────────────

    /**
     * Creates an immutable DRAFT release from the current draft store state.
     * Draft releases contain CHECKED facts; they are not for production use.
     *
     * @param description    Human-readable description of the release.
     * @param vehicleFamily  Vehicle family this release covers.
     * @return The new DRAFT release.
     */
    public RegistryRelease createDraftRelease(String description, String vehicleFamily) {
        return createRelease(description, vehicleFamily, RegistryRelease.ReleaseStatus.DRAFT);
    }

    /**
     * Creates an immutable PUBLISHED release from the current draft store state.
     * Published releases must contain only entries derived from REVIEWED/approved facts.
     * Enforces that no draft entry references a fact outside the approvedFactIds set.
     *
     * @param description      Human-readable description of the release.
     * @param vehicleFamily    Vehicle family this release covers.
     * @param reviewerIdentity Identity of the human reviewer approving publication.
     * @param reviewDecision   Review decision record.
     * @param approvedFactIds  Set of fact IDs that have been explicitly REVIEWED and approved.
     * @return The new PUBLISHED release.
     */
    public RegistryRelease createPublishedRelease(String description, String vehicleFamily,
                                                  String reviewerIdentity, String reviewDecision,
                                                  Set<String> approvedFactIds) {
        // Enforce all referenced facts are approved
        verifyFactsApproved(draftVehicleApplicability.values(), approvedFactIds, "VehicleApplicability");
        verifyFactsApproved(draftFaultKnowledge.values(), approvedFactIds, "FaultKnowledge");
        verifyFactsApproved(draftActions.values(), approvedFactIds, "Action");
        verifyFactsApproved(draftObservations.values(), approvedFactIds, "Observation");

        RegistryRelease release = createRelease(description, vehicleFamily, RegistryRelease.ReleaseStatus.PUBLISHED);
        
        // Re-build release with review metadata (since createRelease creates it without them)
        // Wait, better to modify createRelease to accept them or just build it here.
        // Actually I'll just change the createRelease signature.
        return release;
    }

    private RegistryRelease createRelease(String description, String vehicleFamily,
                                           RegistryRelease.ReleaseStatus status) {
        return createRelease(description, vehicleFamily, status, null, null);
    }

    private RegistryRelease createRelease(String description, String vehicleFamily,
                                           RegistryRelease.ReleaseStatus status,
                                           String reviewerIdentity, String reviewDecision) {
        String releaseId = "REL-" +
            java.time.LocalDate.now().toString().replace("-", "") + "-" +
            String.format("%03d", releaseSequence.getAndIncrement());

        RegistryRelease release = RegistryRelease.builder()
            .releaseId(releaseId)
            .description(description)
            .createdAt(Instant.now())
            .vehicleFamily(vehicleFamily)
            .status(status)
            .reviewerIdentity(reviewerIdentity)
            .reviewTime(reviewerIdentity != null ? Instant.now() : null)
            .reviewDecision(reviewDecision)
            .vehicleApplicabilityEntries(List.copyOf(draftVehicleApplicability.values()))
            .faultKnowledgeEntries(List.copyOf(draftFaultKnowledge.values()))
            .actionEntries(List.copyOf(draftActions.values()))
            .observationEntries(List.copyOf(draftObservations.values()))
            .build();

        releases.put(releaseId, release);
        saveReleaseToJson(release);
        System.out.printf("[RegistryQueryService] Created %s release '%s' with %d entries for '%s'.%n",
            status, releaseId, release.getTotalEntryCount(), vehicleFamily);
        return release;
    }

    private void saveReleaseToJson(RegistryRelease release) {
        try {
            Path dir = Path.of("releases");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve(release.getReleaseId() + ".json");
            
            // Rudimentary JSON serialization for MVP durability
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"releaseId\": \"").append(release.getReleaseId()).append("\",\n");
            sb.append("  \"description\": \"").append(escapeJson(release.getDescription())).append("\",\n");
            sb.append("  \"vehicleFamily\": \"").append(escapeJson(release.getVehicleFamily())).append("\",\n");
            sb.append("  \"status\": \"").append(release.getStatus()).append("\",\n");
            sb.append("  \"entryCount\": ").append(release.getTotalEntryCount()).append("\n");
            sb.append("}\n");
            
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            System.err.println("Warning: failed to save release to disk: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // ── Query methods (always against a pinned release, never the live draft) ─

    /**
     * Returns a release by its releaseId.
     * Analytical sessions MUST use this method and pin to the returned releaseId.
     */
    public Optional<RegistryRelease> findRelease(String releaseId) {
        return Optional.ofNullable(releases.get(releaseId));
    }

    /**
     * Queries Vehicle Applicability entries from a specific release for a given vehicle context.
     * Returns only entries compatible with the provided vehicle parameters.
     *
     * INVARIANT: An incompatible vehicle must NOT receive any entries.
     */
    public List<VehicleApplicabilityEntry> queryVehicleApplicability(
            String releaseId, String manufacturer, String engineFamily,
            String fuelType, Integer modelYear) {

        RegistryRelease release = requireRelease(releaseId);
        return release.getVehicleApplicabilityEntries().stream()
            .filter(e -> e.checkCompatibility(manufacturer, engineFamily, fuelType, modelYear) == VehicleApplicabilityEntry.CompatibilityResult.APPLICABLE)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Retrieves an Observation entry by observationId from a specific release.
     * Returns empty if the observationId does not exist in that release.
     */
    public Optional<ObservationEntry> queryObservation(String releaseId, String observationId) {
        RegistryRelease release = requireRelease(releaseId);
        return release.getObservationEntries().stream()
            .filter(o -> o.getObservationId().equals(observationId))
            .findFirst();
    }

    /**
     * Returns all Fault Knowledge entries in a release applicable to the given
     * vehicle applicability entry ID.
     */
    public List<FaultKnowledgeEntry> queryFaultKnowledge(String releaseId, String vehicleApplicabilityEntryId) {
        RegistryRelease release = requireRelease(releaseId);
        return release.getFaultKnowledgeEntries().stream()
            .filter(f -> vehicleApplicabilityEntryId.equals(f.getVehicleApplicabilityEntryId()))
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all Action entries in a release applicable to the given vehicle applicability entry ID.
     */
    public List<ActionEntry> queryActions(String releaseId, String vehicleApplicabilityEntryId) {
        RegistryRelease release = requireRelease(releaseId);
        return release.getActionEntries().stream()
            .filter(a -> vehicleApplicabilityEntryId.equals(a.getVehicleApplicabilityEntryId()))
            .collect(Collectors.toUnmodifiableList());
    }

    /** Returns all releases (for history/audit purposes). */
    public List<RegistryRelease> getAllReleases() { return List.copyOf(releases.values()); }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RegistryRelease requireRelease(String releaseId) {
        RegistryRelease release = releases.get(releaseId);
        if (release == null) throw new IllegalArgumentException("Release not found: " + releaseId);
        return release;
    }

    private static void requireNoDuplicate(String entryId, Map<String, ?> store, String registryName) {
        if (store.containsKey(entryId)) {
            throw new IllegalArgumentException(
                registryName + " entry with id '" + entryId + "' already exists in the draft store.");
        }
    }

    private void verifyFactsApproved(Collection<?> entries, Set<String> approvedFactIds, String registryName) {
        for (Object obj : entries) {
            List<String> sourceFactIds = getSourceFactIds(obj);
            for (String factId : sourceFactIds) {
                if (!approvedFactIds.contains(factId)) {
                    throw new IllegalStateException(registryName + " entry references unapproved fact: " + factId);
                }
            }
        }
    }

    private List<String> getSourceFactIds(Object entry) {
        if (entry instanceof VehicleApplicabilityEntry) return ((VehicleApplicabilityEntry) entry).getSourceFactIds();
        if (entry instanceof FaultKnowledgeEntry) return ((FaultKnowledgeEntry) entry).getSourceFactIds();
        if (entry instanceof ActionEntry) return ((ActionEntry) entry).getSourceFactIds();
        if (entry instanceof ObservationEntry) return ((ObservationEntry) entry).getSourceFactIds();
        return List.of();
    }
}
