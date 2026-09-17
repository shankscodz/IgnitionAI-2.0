package com.ignitionai.obd;

import java.util.List;

public class ObservationStream {
    public String vehicleId;
    public String sessionStart;
    public List<SensorReadingDto> readings;
}
