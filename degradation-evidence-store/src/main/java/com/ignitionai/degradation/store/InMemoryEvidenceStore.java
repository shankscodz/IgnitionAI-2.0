package com.ignitionai.degradation.store;

import com.ignitionai.phase4.AnomalyEpisode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryEvidenceStore implements DegradationEvidenceStore {

    // Key: vehicleId + "|" + subsystemId
    private Map<String, List<AnomalyEpisode>> store = new HashMap<>();

    @Override
    public void storeEpisode(AnomalyEpisode episode) {
        String key = episode.getVehicleId() + "|" + episode.getSubsystemId();
        store.computeIfAbsent(key, k -> new ArrayList<>()).add(episode);
    }

    @Override
    public List<AnomalyEpisode> getEpisodes(String vehicleId, String subsystemId) {
        String key = vehicleId + "|" + subsystemId;
        return store.getOrDefault(key, new ArrayList<>());
    }

    @Override
    public List<AnomalyEpisode> getEpisodesSince(String vehicleId, String subsystemId, Long sinceMs) {
        return getEpisodes(vehicleId, subsystemId).stream()
                .filter(e -> e.getStartTimeMs() >= sinceMs)
                .collect(Collectors.toList());
    }
}
