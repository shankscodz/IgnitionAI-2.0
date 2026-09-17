package com.ignitionai.virtualsensors;

import java.util.Map;
import com.ignitionai.context.VehicleContext;

public interface VirtualSensorPlugin {
    String getSensorId();
    SensorOutput execute(Map<String, Double> inputs, VehicleContext context);
}
