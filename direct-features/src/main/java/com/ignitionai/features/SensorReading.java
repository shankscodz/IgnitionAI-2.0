package com.ignitionai.features;

import java.util.ArrayList;
import java.util.List;

public class SensorReading {
    private final String signalId;
    private final Double value;
    private final Long timestampMs;
    private final String units;

    public SensorReading(String signalId, Double value, Long timestampMs, String units) {
        this.signalId = signalId;
        this.value = value;
        this.timestampMs = timestampMs;
        this.units = units;
    }

    public String getSignalId() { return signalId; }
    public Double getValue() { return value; }
    public Long getTimestampMs() { return timestampMs; }
    public String getUnits() { return units; }
}
