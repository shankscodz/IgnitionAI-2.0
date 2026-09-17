package com.ignitionai.healthevidence;

import java.util.ArrayList;
import java.util.List;

public class HealthEvidenceStore {
    private final List<HealthEvidenceRecord> store = new ArrayList<>();

    public void addEvidence(HealthEvidenceRecord record) {
        store.add(record);
    }

    public List<HealthEvidenceRecord> getEvidenceForVehicle(String vehicleId) {
        List<HealthEvidenceRecord> results = new ArrayList<>();
        for (HealthEvidenceRecord r : store) {
            if (r.getVehicleId().equals(vehicleId)) {
                results.add(r);
            }
        }
        return results;
    }
    
    public List<HealthEvidenceRecord> getAllEvidence() {
        return new ArrayList<>(store);
    }
}
