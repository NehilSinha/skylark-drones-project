package com.skylark.skylarkbiagentbackend.normalizer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumericParserTest {

    @Test
    void parseAmount_cleanNumber() {
        assertThat(NumericParser.parseAmount("489360")).contains(new BigDecimal("489360"));
    }

    @Test
    void parseAmount_commaFormatted() {
        assertThat(NumericParser.parseAmount("17,616,960")).contains(new BigDecimal("17616960"));
    }

    @Test
    void parseAmount_blankIsEmpty_notZero() {
        assertThat(NumericParser.parseAmount("")).isEmpty();
        assertThat(NumericParser.parseAmount(null)).isEmpty();
    }

    @Test
    void parseAmount_completelyNonNumericIsEmpty() {
        assertThat(NumericParser.parseAmount("N/A")).isEmpty();
    }

    @Test
    void parseAmount_extractsNumericPortionFromMixedText() {
        // e.g. "5360 HA" as seen in the real Work Order Tracker export
        assertThat(NumericParser.parseAmount("5360 HA")).contains(new BigDecimal("5360"));
    }
}
