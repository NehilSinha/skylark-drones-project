package com.skylark.skylarkbiagentbackend.normalizer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Defensive date parsing. The source CSVs use ISO format almost exclusively, but
 * "bad dates" are explicitly called out as a normalization requirement, so a couple
 * of common alternate formats are tried before giving up. Unparseable/blank input
 * returns {@code null}, never a fabricated date.
 */
public final class DateParser {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    private DateParser() {
    }

    public static LocalDate parse(String raw) {
        String normalized = TextNormalizationUtils.normalizeToNull(raw);
        if (normalized == null) {
            return null;
        }
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }
}
