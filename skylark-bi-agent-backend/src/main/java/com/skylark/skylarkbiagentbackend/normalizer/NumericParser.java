package com.skylark.skylarkbiagentbackend.normalizer;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defensive numeric parsing for Monday column text. Never returns zero for
 * unparseable input — an unparseable amount is "unknown," not "nothing," and a
 * silent zero would understate whatever it's summed into (see PHASE-4-DESIGN.md
 * §0). Callers are expected to exclude {@code Optional.empty()} results from
 * totals and count them as a data-quality warning instead.
 */
public final class NumericParser {

    private static final Pattern NUMERIC_PORTION = Pattern.compile("-?\\d+(\\.\\d+)?");

    private NumericParser() {
    }

    public static Optional<BigDecimal> parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String cleaned = raw.replace(",", "").trim();
        try {
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            Matcher matcher = NUMERIC_PORTION.matcher(cleaned);
            if (!matcher.find()) {
                return Optional.empty();
            }
            try {
                return Optional.of(new BigDecimal(matcher.group()));
            } catch (NumberFormatException stillUnparseable) {
                return Optional.empty();
            }
        }
    }
}
