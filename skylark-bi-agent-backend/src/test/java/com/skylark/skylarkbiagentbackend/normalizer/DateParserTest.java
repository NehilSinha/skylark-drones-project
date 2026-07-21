package com.skylark.skylarkbiagentbackend.normalizer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateParserTest {

    @Test
    void parse_isoFormat() {
        assertThat(DateParser.parse("2025-12-26")).isEqualTo(LocalDate.of(2025, 12, 26));
    }

    @Test
    void parse_slashFormat() {
        assertThat(DateParser.parse("26/12/2025")).isEqualTo(LocalDate.of(2025, 12, 26));
    }

    @Test
    void parse_blankOrNullReturnsNull() {
        assertThat(DateParser.parse("")).isNull();
        assertThat(DateParser.parse(null)).isNull();
    }

    @Test
    void parse_garbageReturnsNull_notAFabricatedDate() {
        assertThat(DateParser.parse("not a date")).isNull();
    }
}
