package com.ignitionai.virtualsensors;

import java.util.Map;
import com.ignitionai.context.VehicleContext;
import com.ignitionai.obd.AnalyticalObservation;

public interface VirtualSensorPlugin {
    String getSensorId();
    AnalyticalObservation execute(Map<String, AnalyticalObservation> inputs, VehicleContext context);
}
