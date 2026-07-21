package com.skylark.skylarkbiagentbackend.dto;

/**
 * A non-fatal data quality issue surfaced alongside an otherwise-successful
 * analytics result — e.g. "181 of 346 deals excluded: missing revenue". These are
 * never exceptions (see ADR-001 "Error Handling Strategy"): incomplete-but-usable
 * source data must never fail a request outright, it must be disclosed instead.
 */
public record DataQualityWarning(
        Severity severity,
        String code,
        String message,
        long affectedRecordCount
) {

    public enum Severity {
        INFO, LOW, MEDIUM, HIGH
    }

    public static DataQualityWarning of(Severity severity, String code, String message, long affectedRecordCount) {
        return new DataQualityWarning(severity, code, message, affectedRecordCount);
    }
}
