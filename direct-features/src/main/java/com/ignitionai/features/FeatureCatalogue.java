package com.ignitionai.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FeatureCatalogue {
    private Map<String, Feature> features = new HashMap<>();

    public void register(Feature feature) {
        features.put(feature.getFeatureId(), feature);
    }

    public Feature getFeature(String featureId) {
        return features.get(featureId);
    }

    public List<Feature> getAllFeatures() {
        return new ArrayList<>(features.values());
    }
}
