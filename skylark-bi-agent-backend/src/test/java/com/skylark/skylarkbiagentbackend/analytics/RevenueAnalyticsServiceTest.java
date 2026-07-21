package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.dto.DateRange;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.DealStatus;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevenueAnalyticsServiceTest {

    private DealQueryService dealQueryService;
    private RevenueAnalyticsService service;

    @BeforeEach
    void setUp() {
        dealQueryService = Mockito.mock(DealQueryService.class);
        service = new RevenueAnalyticsService(dealQueryService);
    }

    @Test
    void summary_sumsWonDealsOnly_excludingOpenAndBlankValue() {
        NormalizedDeal won1 = deal("A", DealStatus.WON, new BigDecimal("100000"), LocalDate.of(2026, 8, 15));
        NormalizedDeal won2 = deal("B", DealStatus.WON, new BigDecimal("200000"), LocalDate.of(2026, 6, 1));
        NormalizedDeal wonBlankValue = deal("C", DealStatus.WON, null, LocalDate.of(2026, 8, 1));
        NormalizedDeal open = deal("D", DealStatus.OPEN, new BigDecimal("500000"), null);
        NormalizedDeal wonNoCloseDate = deal("E", DealStatus.WON, new BigDecimal("50000"), null);

        when(dealQueryService.queryAll(any())).thenReturn(List.of(won1, won2, wonBlankValue, open, wonNoCloseDate));

        RevenueSummaryResponse response = service.summary(null);

        assertThat(response.bookedRevenue()).isEqualByComparingTo("350000"); // A + B + E, D excluded (not Won)
        assertThat(response.billedRevenue()).isNull();
        assertThat(response.collectedRevenue()).isNull();
        assertThat(response.outstandingReceivable()).isNull();

        assertThat(response.warnings()).hasSize(2);
        assertThat(response.warnings()).anyMatch(w -> "MISSING_DEAL_VALUE".equals(w.code()) && w.affectedRecordCount() == 1);
        assertThat(response.warnings()).anyMatch(w -> "WORK_ORDER_DATA_UNAVAILABLE".equals(w.code()));
    }

    @Test
    void summary_dateRangeFilter_matchesActualCloseDateNotTentativeCloseDate() {
        NormalizedDeal inRange = deal("A", DealStatus.WON, new BigDecimal("100000"), LocalDate.of(2026, 8, 15));
        NormalizedDeal outOfRange = deal("B", DealStatus.WON, new BigDecimal("200000"), LocalDate.of(2026, 6, 1));
        NormalizedDeal noCloseDate = deal("C", DealStatus.WON, new BigDecimal("999999"), null);

        when(dealQueryService.queryAll(any())).thenReturn(List.of(inRange, outOfRange, noCloseDate));

        EntityFilter filter = new EntityFilter(null, null, null, null,
                new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "August"));

        RevenueSummaryResponse response = service.summary(filter);

        assertThat(response.bookedRevenue()).isEqualByComparingTo("100000");
    }

    @Test
    void summary_ignoresCallerSuppliedDealStatusesAndDateRange_whenQueryingDealQueryService() {
        EntityFilter filter = new EntityFilter(List.of("Mining"), List.of("OWNER_001"), null,
                List.of("Open"), new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "2026"));

        when(dealQueryService.queryAll(any())).thenReturn(List.of());

        service.summary(filter);

        ArgumentCaptor<EntityFilter> captor = ArgumentCaptor.forClass(EntityFilter.class);
        verify(dealQueryService).queryAll(captor.capture());

        EntityFilter passedFilter = captor.getValue();
        assertThat(passedFilter.sectors()).containsExactly("Mining");
        assertThat(passedFilter.owners()).containsExactly("OWNER_001");
        assertThat(passedFilter.dealStatuses()).isNull();
        assertThat(passedFilter.dateRange()).isNull();
    }

    @Test
    void summary_noWonDeals_returnsZeroBookedRevenueNotAnException() {
        when(dealQueryService.queryAll(any())).thenReturn(List.of(
                deal("A", DealStatus.OPEN, new BigDecimal("100000"), null)));

        RevenueSummaryResponse response = service.summary(null);

        assertThat(response.bookedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private NormalizedDeal deal(String id, DealStatus status, BigDecimal value, LocalDate closeDate) {
        return new NormalizedDeal(id, "Deal " + id, "OWNER_001", "COMPANY001", status, closeDate, null,
                value, null, "G. Project Won", null, "Mining", null);
    }
}
