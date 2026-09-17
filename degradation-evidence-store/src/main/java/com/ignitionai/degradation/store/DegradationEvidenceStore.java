package com.ignitionai.degradation.store;

import com.ignitionai.phase4.AnomalyEpisode;
import java.util.List;

public interface DegradationEvidenceStore {
    void storeEpisode(AnomalyEpisode episode);
    List<AnomalyEpisode> getEpisodes(String vehicleId, String subsystemId);
    List<AnomalyEpisode> getEpisodesSince(String vehicleId, String subsystemId, Long sinceMs);
}
