package tools.pipeline_runner;

import com.ignitionai.obd.ObservationStream;
import com.ignitionai.obd.SensorReadingDto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObdInputParser {
    // Custom MVP parser for obd-input.v1 schema array
    public static ObservationStream parse(File file) throws Exception {
        ObservationStream stream = new ObservationStream();
        stream.readings = new ArrayList<>();
        stream.vehicleId = "UNKNOWN";

        Pattern sigIdPattern = Pattern.compile("\"signal_id\"\\s*:\\s*\"([^\"]+)\"");
        Pattern valPattern = Pattern.compile("\"value\"\\s*:\\s*([0-9.-]+)");
        Pattern unitPattern = Pattern.compile("\"unit\"\\s*:\\s*\"([^\"]+)\"");
        Pattern tsPattern = Pattern.compile("\"monotonic_ms\"\\s*:\\s*([0-9]+)");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("\"vehicle_id\"")) {
                    stream.vehicleId = line.split(":")[1].replaceAll("\"", "").replaceAll(",", "").trim();
                } else if (line.contains("\"signal_id\"") && line.contains("{")) {
                    SensorReadingDto currentDto = new SensorReadingDto();
                    
                    Matcher mId = sigIdPattern.matcher(line);
                    if (mId.find()) currentDto.signalId = mId.group(1);
                    
                    Matcher mVal = valPattern.matcher(line);
                    if (mVal.find()) currentDto.value = Double.parseDouble(mVal.group(1));
                    
                    Matcher mUnit = unitPattern.matcher(line);
                    if (mUnit.find()) currentDto.units = mUnit.group(1);
                    
                    Matcher mTs = tsPattern.matcher(line);
                    if (mTs.find()) currentDto.timestampMs = Long.parseLong(mTs.group(1));
                    
                    currentDto.quality = "valid";
                    
                    if (currentDto.signalId != null && currentDto.value != null && currentDto.timestampMs != null) {
                        stream.readings.add(currentDto);
                    }
                }
            }
        }
        
        // Ensure chronological order
        stream.readings.sort(Comparator.comparing(r -> r.timestampMs));
        return stream;
    }
}
