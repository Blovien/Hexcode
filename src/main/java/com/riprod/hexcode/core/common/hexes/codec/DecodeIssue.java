package com.riprod.hexcode.core.common.hexes.codec;

public class DecodeIssue {

    public enum Severity { INFO, WARNING, ERROR }

    private final String message;
    private final Severity severity;

    public DecodeIssue(String message, Severity severity) {
        this.message = message;
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "[" + severity.name().toLowerCase() + "] " + message;
    }
}
