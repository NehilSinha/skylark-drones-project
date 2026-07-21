package com.skylark.skylarkbiagentbackend.normalizer;

/**
 * The one shared text-cleanup rule every normalizer applies: trim, collapse
 * internal whitespace, treat blank as absent. See PHASE-4-DESIGN.md §0
 * "Normalization contract."
 */
public final class TextNormalizationUtils {

    private TextNormalizationUtils() {
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
    }

    /** Same as {@link #normalize(String)}, but returns {@code null} for blank input
     * instead of an empty string — the common case for optional fields, where
     * "missing" should be represented as {@code null}, not an empty string that
     * looks present. */
    public static String normalizeToNull(String raw) {
        String normalized = normalize(raw);
        return normalized.isEmpty() ? null : normalized;
    }
}
