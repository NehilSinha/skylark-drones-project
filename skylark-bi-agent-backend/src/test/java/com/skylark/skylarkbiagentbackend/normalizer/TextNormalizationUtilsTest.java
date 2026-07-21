package com.skylark.skylarkbiagentbackend.normalizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizationUtilsTest {

    @Test
    void normalize_trimsAndCollapsesInternalWhitespace() {
        assertThat(TextNormalizationUtils.normalize("  Mining   Sector  ")).isEqualTo("Mining Sector");
    }

    @Test
    void normalize_nullBecomesEmptyString() {
        assertThat(TextNormalizationUtils.normalize(null)).isEmpty();
    }

    @Test
    void normalizeToNull_blankBecomesNull() {
        assertThat(TextNormalizationUtils.normalizeToNull("   ")).isNull();
        assertThat(TextNormalizationUtils.normalizeToNull(null)).isNull();
    }

    @Test
    void normalizeToNull_preservesNonBlankContent() {
        assertThat(TextNormalizationUtils.normalizeToNull(" Renewables ")).isEqualTo("Renewables");
    }
}
