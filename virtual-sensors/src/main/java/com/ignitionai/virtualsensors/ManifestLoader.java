package com.ignitionai.virtualsensors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManifestLoader {

    public List<SensorDefinition> loadManifests(File directory) {
        List<SensorDefinition> definitions = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return definitions;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".properties") || name.endsWith(".manifest"));
        if (files == null) return definitions;

        for (File file : files) {
            try {
                definitions.add(parseManifest(file));
            } catch (Exception e) {
                System.err.println("Failed to load manifest: " + file.getName() + " - " + e.getMessage());
            }
        }
        return definitions;
    }

    private SensorDefinition parseManifest(File file) throws Exception {
        SensorDefinition def = new SensorDefinition();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "sensor_id": def.setSensorId(value); break;
                    case "sensor_version": def.setSensorVersion(value); break;
                    case "display_name": def.setDisplayName(value); break;
                    case "observation_kind": def.setObservationKind(value); break;
                    case "input_signal_ids": def.setInputSignalIds(parseList(value)); break;
                    case "input_feature_ids": def.setInputFeatureIds(parseList(value)); break;
                    case "output_signal_ids": def.setOutputSignalIds(parseList(value)); break;
                    case "units": def.setUnits(value); break;
                    case "formula_or_model_reference": def.setFormulaOrModelReference(value); break;
                    case "sampling_requirements": def.setSamplingRequirements(value); break;
                    case "uncertainty_definition": def.setUncertaintyDefinition(value); break;
                    case "implementation_type": def.setImplementationType(value); break;
                    case "status": def.setStatus(value); break;
                }
            }
        }
        return def;
    }

    private List<String> parseList(String value) {
        if (value.isEmpty()) return new ArrayList<>();
        return Arrays.asList(value.split(","));
    }
}
