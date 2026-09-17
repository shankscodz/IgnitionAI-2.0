package com.ignitionai.obdgenerator;

import com.ignitionai.obdgenerator.config.GeneratorConfig;
import com.ignitionai.obdgenerator.generator.GenerationResult;
import com.ignitionai.obdgenerator.generator.ObdGenerator;
import com.ignitionai.obdinput.schema.ObdMessage;

import java.util.List;
import java.util.Map;

public class GeneratorTest {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("=== obd-generator Tests ===");
        
        if (test_generatorDeterminism()) passed++; else failed++;
        if (test_newScenarioViaConfig()) passed++; else failed++;

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean test_generatorDeterminism() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setVehicleId("VEH-TATA-NEXON-0001");
            config.setSupportedSignals(List.of("engine_rpm"));
            config.setSamplingRatesHz(Map.of("engine_rpm", 10.0));
            config.setDurationMs(1000);
            config.setRandomSeed(12345);
            
            ObdGenerator gen1 = new ObdGenerator(config);
            GenerationResult res1 = gen1.generate();
            
            ObdGenerator gen2 = new ObdGenerator(config);
            GenerationResult res2 = gen2.generate();
            
            if (res1.getPublicStream().size() == res2.getPublicStream().size()) {
                double val1 = res1.getPublicStream().get(0).getSensorReadings().get(0).getValue();
                double val2 = res2.getPublicStream().get(0).getSensorReadings().get(0).getValue();
                if (Double.compare(val1, val2) == 0) {
                    System.out.println("  PASS  test_generatorDeterminism");
                    return true;
                }
            }
            System.out.println("  FAIL  test_generatorDeterminism");
            return false;
        } catch (Exception e) {
            System.out.println("  FAIL  test_generatorDeterminism: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean test_newScenarioViaConfig() {
        try {
            GeneratorConfig config = new GeneratorConfig();
            config.setSupportedSignals(List.of("vehicle_speed"));
            config.setDurationMs(500);
            config.setRandomSeed(999);
            config.setMissingnessProbability(1.0); // 100% missing data
            
            ObdGenerator gen = new ObdGenerator(config);
            GenerationResult res = gen.generate();
            
            if (res.getPublicStream().isEmpty()) { // since all missing, empty output
                System.out.println("  PASS  test_newScenarioViaConfig");
                return true;
            } else {
                System.out.println("  FAIL  test_newScenarioViaConfig: Expected empty stream");
                return false;
            }
        } catch (Exception e) {
            System.out.println("  FAIL  test_newScenarioViaConfig: " + e.getMessage());
            return false;
        }
    }
}
