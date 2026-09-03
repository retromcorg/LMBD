package com.johnymuffin.jban.core.evidence;

public enum EvidenceVisibility {
    PUBLIC,
    AUTHENTICATED,
    STAFF_ONLY;

    public static EvidenceVisibility fromValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return PUBLIC;
        }

        try {
            return EvidenceVisibility.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PUBLIC;
        }
    }
}
