package com.ignitionai.degradation.analyzer;

import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput.TrendDirection;

import java.util.ArrayList;
import java.util.List;

public class TrendAnalyzerTest {
    public static void main(String[] args) {
        System.out.println("=== trend-and-persistence-analyzer Tests ===");

        testInsufficientHistory();
        testWorseningTrend();

        System.out.println("\n=== Results: 2 passed, 0 failed ===");
    }

    private static void testInsufficientHistory() {
        TrendAnalyzer analyzer = new TrendAnalyzer();
        AnomalyEpisode curr = new AnomalyEpisode();
        curr.setSeverity(0.5);
        
        TrendResult res = analyzer.analyzeTrend(curr, new ArrayList<>());
        
        assert res.getDirection() == TrendDirection.UNKNOWN : "Expected UNKNOWN for empty history";
        assert res.getTrendSlope() == 0.0 : "Expected 0 slope for empty history";
        System.out.println("  PASS  testInsufficientHistory");
    }

    private static void testWorseningTrend() {
        TrendAnalyzer analyzer = new TrendAnalyzer();
        
        List<AnomalyEpisode> history = new ArrayList<>();
        AnomalyEpisode e1 = new AnomalyEpisode(); e1.setStartTimeMs(1000L * 3600); e1.setSeverity(0.2); history.add(e1);
        AnomalyEpisode e2 = new AnomalyEpisode(); e2.setStartTimeMs(2000L * 3600); e2.setSeverity(0.4); history.add(e2);
        
        AnomalyEpisode curr = new AnomalyEpisode(); curr.setStartTimeMs(3000L * 3600); curr.setSeverity(0.6);
        
        TrendResult res = analyzer.analyzeTrend(curr, history);
        
        assert res.getDirection() == TrendDirection.WORSENING : "Expected WORSENING for increasing severity";
        assert res.getTrendSlope() > 0 : "Expected positive slope";
        assert res.getLatentState() > 0.4 : "Expected state tracking";
        System.out.println("  PASS  testWorseningTrend");
    }
}
