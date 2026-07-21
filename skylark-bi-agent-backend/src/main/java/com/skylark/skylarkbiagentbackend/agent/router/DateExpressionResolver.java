package com.skylark.skylarkbiagentbackend.agent.router;

import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Converts a natural-language date phrase into a concrete {@link DateRange}. This
 * is the one place date arithmetic happens in the whole application — every
 * analytics tool receives an already-resolved range, never a phrase (see
 * PHASE-4-DESIGN.md T1 / ADR-001 "AI Agent Workflow").
 *
 * <p>"This quarter"/"this year"/"YTD" resolve against the <b>Indian fiscal year
 * (Apr–Mar)</b>, confirmed 2026-07-21 — see PHASE-4-DESIGN.md §6.
 */
@Component
public class DateExpressionResolver {

    private static final int FISCAL_YEAR_START_MONTH = 4;

    private final Clock clock;

    public DateExpressionResolver(Clock clock) {
        this.clock = clock;
    }

    public DateRange resolve(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            throw new ValidationException("Date expression must not be blank");
        }
        LocalDate today = LocalDate.now(clock);
        String normalized = phrase.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "today" -> new DateRange(today, today, "today");
            case "yesterday" -> {
                LocalDate y = today.minusDays(1);
                yield new DateRange(y, y, "yesterday");
            }
            case "this week" -> thisWeek(today, "this week");
            case "last week" -> thisWeek(today.minusWeeks(1), "last week");
            case "this month" -> new DateRange(
                    today.withDayOfMonth(1), today.with(TemporalAdjusters.lastDayOfMonth()), "this month");
            case "last month" -> {
                LocalDate lastMonth = today.minusMonths(1);
                yield new DateRange(
                        lastMonth.withDayOfMonth(1), lastMonth.with(TemporalAdjusters.lastDayOfMonth()), "last month");
            }
            case "this quarter" -> fiscalQuarter(today, "this quarter");
            case "last quarter" -> {
                DateRange current = fiscalQuarter(today, "this quarter");
                yield fiscalQuarter(current.start().minusDays(1), "last quarter");
            }
            case "this year" -> fiscalYear(today, "this year");
            case "last year" -> fiscalYear(fiscalYear(today, "this year").start().minusDays(1), "last year");
            case "year to date", "ytd" -> new DateRange(fiscalYear(today, "").start(), today, "year to date");
            case "next 30 days" -> new DateRange(today, today.plusDays(30), "next 30 days");
            case "overdue" -> new DateRange(LocalDate.of(1970, 1, 1), today.minusDays(1), "overdue");
            case "upcoming deliveries", "upcoming" -> new DateRange(today, today.plusDays(30), "upcoming deliveries");
            default -> throw new ValidationException(
                    "Unrecognized date expression: \"" + phrase + "\". Ask the user for a specific range.");
        };
    }

    private DateRange thisWeek(LocalDate reference, String label) {
        LocalDate monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return new DateRange(monday, sunday, label);
    }

    /** FY start year for a given date: Apr–Dec belongs to the FY starting that same calendar year; Jan–Mar belongs to the FY that started the previous calendar year. */
    private int fiscalYearStartYear(LocalDate date) {
        return date.getMonthValue() >= FISCAL_YEAR_START_MONTH ? date.getYear() : date.getYear() - 1;
    }

    private DateRange fiscalYear(LocalDate reference, String label) {
        int startYear = fiscalYearStartYear(reference);
        LocalDate start = LocalDate.of(startYear, FISCAL_YEAR_START_MONTH, 1);
        LocalDate end = LocalDate.of(startYear + 1, FISCAL_YEAR_START_MONTH, 1).minusDays(1);
        return new DateRange(start, end, label);
    }

    private DateRange fiscalQuarter(LocalDate reference, String label) {
        LocalDate fyStart = fiscalYear(reference, "").start();
        long monthsSinceFyStart = ChronoUnit.MONTHS.between(fyStart.withDayOfMonth(1), reference.withDayOfMonth(1));
        long quarterIndex = monthsSinceFyStart / 3;
        LocalDate quarterStart = fyStart.plusMonths(quarterIndex * 3);
        LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
        return new DateRange(quarterStart, quarterEnd, label);
    }
}
