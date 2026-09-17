package com.ignitionai.technicalsources.schema;

/**
 * Type of technical document.
 *
 * Used to determine authority and applicability rules.
 * A credible document for the wrong vehicle type must not govern another vehicle's analysis.
 */
public enum DocumentType {
    SERVICE_MANUAL,
    TECHNICAL_BULLETIN,
    WIRING_DIAGRAM,
    PARTS_CATALOGUE,
    EMISSIONS_SPECIFICATION,
    ENGINEERING_SPECIFICATION,
    DIAGNOSTIC_PROCEDURE,
    TECHNICAL_TRAINING_MATERIAL,
    REGULATORY_DOCUMENT,
    OTHER
}
