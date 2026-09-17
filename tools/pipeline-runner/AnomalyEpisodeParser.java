package tools.pipeline_runner;

import com.ignitionai.phase4.AnomalyEpisode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnomalyEpisodeParser {

    public static List<AnomalyEpisode> parse(File file) throws Exception {
        List<AnomalyEpisode> episodes = new ArrayList<>();

        Pattern vidPattern = Pattern.compile("\"vehicleId\"\\s*:\\s*\"([^\"]+)\"");
        Pattern sidPattern = Pattern.compile("\"sessionId\"\\s*:\\s*\"([^\"]+)\"");
        Pattern subPattern = Pattern.compile("\"subsystemId\"\\s*:\\s*\"([^\"]+)\"");
        Pattern eidPattern = Pattern.compile("\"episodeId\"\\s*:\\s*\"([^\"]+)\"");
        Pattern startPattern = Pattern.compile("\"startTimeMs\"\\s*:\\s*([0-9]+)");
        Pattern endPattern = Pattern.compile("\"endTimeMs\"\\s*:\\s*([0-9]+)");
        Pattern sevPattern = Pattern.compile("\"severity\"\\s*:\\s*([0-9.-]+)");
        Pattern durPattern = Pattern.compile("\"durationMs\"\\s*:\\s*([0-9.]+)");
        Pattern recPattern = Pattern.compile("\"recurrenceCount\"\\s*:\\s*([0-9]+)");
        Pattern persPattern = Pattern.compile("\"persistence\"\\s*:\\s*([0-9.-]+)");
        Pattern regPattern = Pattern.compile("\"operatingRegime\"\\s*:\\s*\"([^\"]+)\"");
        Pattern confPattern = Pattern.compile("\"confidence\"\\s*:\\s*([0-9.-]+)");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            AnomalyEpisode current = null;
            
            while ((line = br.readLine()) != null) {
                if (line.contains("\"vehicleId\"")) {
                    current = new AnomalyEpisode();
                    Matcher m = vidPattern.matcher(line); 
                    if (m.find()) current.setVehicleId(m.group(1));
                }
                
                if (current != null) {
                    Matcher m = sidPattern.matcher(line); if (m.find()) current.setSessionId(m.group(1));
                    m = subPattern.matcher(line); if (m.find()) current.setSubsystemId(m.group(1));
                    m = eidPattern.matcher(line); if (m.find()) current.setEpisodeId(m.group(1));
                    m = startPattern.matcher(line); if (m.find()) current.setStartTimeMs(Long.parseLong(m.group(1)));
                    m = endPattern.matcher(line); if (m.find()) current.setEndTimeMs(Long.parseLong(m.group(1)));
                    m = sevPattern.matcher(line); if (m.find()) current.setSeverity(Double.parseDouble(m.group(1)));
                    m = durPattern.matcher(line); if (m.find()) current.setDurationMs(Double.parseDouble(m.group(1)));
                    m = recPattern.matcher(line); if (m.find()) current.setRecurrenceCount(Integer.parseInt(m.group(1)));
                    m = persPattern.matcher(line); if (m.find()) current.setPersistence(Double.parseDouble(m.group(1)));
                    m = regPattern.matcher(line); if (m.find()) current.setOperatingRegime(m.group(1));
                    
                    m = confPattern.matcher(line); 
                    if (m.find()) {
                        Map<String, Double> qi = new HashMap<>();
                        qi.put("confidence", Double.parseDouble(m.group(1)));
                        current.setQualityIndicators(qi);
                    }
                    
                    Pattern calPattern = Pattern.compile("\"calibrationVersion\"\\s*:\\s*\"([^\"]+)\"");
                    m = calPattern.matcher(line); if (m.find()) current.setCalibrationVersion(m.group(1));
                    
                    Pattern maintPattern = Pattern.compile("\"maintenanceAction\"\\s*:\\s*\"([^\"]+)\"");
                    m = maintPattern.matcher(line); if (m.find()) current.setMaintenanceAction(m.group(1));
                }
                
                if (line.contains("}") && !line.contains("{") && current != null && current.getVehicleId() != null) {
                    current.setContextVersion("1.0");
                    current.setModelVersion("1.0");
                    episodes.add(current);
                    current = null;
                }
            }
        }
        
        return episodes;
    }
}
