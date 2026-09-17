package com.ignitionai.virtualsensors;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignitionai.obd.AnalyticalObservation;

public class MathExpressionEvaluator {
    // A very simple evaluator for MVP. Supports replacing variables and evaluating basic expressions.
    // Supports +, -, *, /, and diff(a,b).

    public static Double evaluate(String formula, Map<String, AnalyticalObservation> inputs) {
        if (formula == null || formula.isEmpty()) return null;
        
        // Handle diff(a, b) special case
        if (formula.startsWith("diff(") && formula.endsWith(")")) {
            String inner = formula.substring(5, formula.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length == 2) {
                AnalyticalObservation a = inputs.get(parts[0].trim());
                AnalyticalObservation b = inputs.get(parts[1].trim());
                if (a != null && b != null && a.getValue() != null && b.getValue() != null) {
                    return a.getValue() - b.getValue();
                }
            }
            return null;
        }

        // Replace variables with their double values
        String expr = formula;
        for (Map.Entry<String, AnalyticalObservation> entry : inputs.entrySet()) {
            if (entry.getValue().getValue() != null) {
                expr = expr.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue().getValue().toString());
            }
        }

        // Extremely basic math evaluation (e.g. "a / b" or "a - b" or "a + b")
        // For MVP, we'll just handle one operator
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            if (parts.length == 2) {
                try { return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim()); } catch(Exception e) {}
            }
        } else if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            if (parts.length == 2) {
                try { return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim()); } catch(Exception e) {}
            }
        } else if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            if (parts.length == 2) {
                try { return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim()); } catch(Exception e) {}
            }
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-");
            if (parts.length == 2) {
                try { return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim()); } catch(Exception e) {}
            }
        }

        return null;
    }
}
