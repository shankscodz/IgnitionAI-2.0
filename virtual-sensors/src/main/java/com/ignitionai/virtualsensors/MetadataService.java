package com.ignitionai.virtualsensors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetadataService {
    private final VirtualSensorRuntime runtime;

    public MetadataService(VirtualSensorRuntime runtime) {
        this.runtime = runtime;
    }

    public List<SensorDefinition> getAllRegisteredSensors() {
        return new ArrayList<>(runtime.getRegisteredSensors().values());
    }

    public SensorDefinition getSensorMetadata(String sensorId) {
        return runtime.getRegisteredSensors().get(sensorId);
    }
}
