package com.skylark.skylarkbiagentbackend.agent.router;

import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "Today" is fixed at 2026-07-21 (a Tuesday) unless a test explicitly needs a
 * different reference date to exercise a fiscal-year boundary. Indian fiscal year
 * (Apr-Mar) per PHASE-4-DESIGN.md §6 — this is the single highest-leverage thing to
 * get right, since every date-scoped tool depends on it transitively.
 */
class DateExpressionResolverTest {

    private final DateExpressionResolver resolver = new DateExpressionResolver(fixedClockAt(2026, 7, 21));

    @Test
    void today() {
        DateRange range = resolver.resolve("today");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    void yesterday() {
        DateRange range = resolver.resolve("yesterday");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void thisWeek_isMondayToSundayContainingToday() {
        DateRange range = resolver.resolve("this week");
        assertThat(range.start().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(range.end().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(LocalDate.of(2026, 7, 21)).isBetween(range.start(), range.end());
    }

    @Test
    void lastWeek_isTheWeekBeforeThisWeek() {
        DateRange thisWeek = resolver.resolve("this week");
        DateRange lastWeek = resolver.resolve("last week");
        assertThat(lastWeek.end()).isEqualTo(thisWeek.start().minusDays(1));
    }

    @Test
    void thisMonth() {
        DateRange range = resolver.resolve("this month");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void lastMonth() {
        DateRange range = resolver.resolve("last month");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void thisQuarter_fiscalQ2_julToSep() {
        DateRange range = resolver.resolve("this quarter");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void lastQuarter_fiscalQ1_aprToJun() {
        DateRange range = resolver.resolve("last quarter");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void thisYear_fiscalYearAprToMar() {
        DateRange range = resolver.resolve("this year");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    void lastYear_priorFiscalYear() {
        DateRange range = resolver.resolve("last year");
        assertThat(range.start()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void yearToDate_fiscalYearStartToToday() {
        DateRange range = resolver.resolve("year to date");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 7, 21));

        assertThat(resolver.resolve("ytd")).isEqualTo(range);
    }

    @Test
    void next30Days() {
        DateRange range = resolver.resolve("next 30 days");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void overdue_isEverythingBeforeToday() {
        DateRange range = resolver.resolve("overdue");
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(range.start()).isBefore(range.end());
    }

    @Test
    void upcomingDeliveries() {
        DateRange range = resolver.resolve("upcoming deliveries");
        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void caseAndWhitespaceInsensitive() {
        assertThat(resolver.resolve("  TODAY  ")).isEqualTo(resolver.resolve("today"));
    }

    @Test
    void unrecognizedPhrase_throwsValidationException() {
        assertThatThrownBy(() -> resolver.resolve("next fiscal decade"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankPhrase_throwsValidationException() {
        assertThatThrownBy(() -> resolver.resolve("")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void fiscalYearBoundary_referenceDateInMarch_belongsToPreviousFiscalYear() {
        // 2026-03-15 is in FY2025-26 (Apr 2025 - Mar 2026), NOT FY2026-27 -
        // the one case a naive calendar-year check would get wrong.
        DateExpressionResolver marchResolver = new DateExpressionResolver(fixedClockAt(2026, 3, 15));

        DateRange thisYear = marchResolver.resolve("this year");
        assertThat(thisYear.start()).isEqualTo(LocalDate.of(2025, 4, 1));
        assertThat(thisYear.end()).isEqualTo(LocalDate.of(2026, 3, 31));

        DateRange thisQuarter = marchResolver.resolve("this quarter");
        assertThat(thisQuarter.start()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(thisQuarter.end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    private static Clock fixedClockAt(int year, int month, int day) {
        Instant instant = LocalDate.of(year, month, day).atTime(12, 0).toInstant(ZoneOffset.UTC);
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
