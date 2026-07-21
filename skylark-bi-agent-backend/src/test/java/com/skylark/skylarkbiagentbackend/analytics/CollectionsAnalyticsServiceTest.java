package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CollectionsAnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);

    private WorkOrderQueryService workOrderQueryService;
    private CollectionsAnalyticsService service;

    @BeforeEach
    void setUp() {
        workOrderQueryService = Mockito.mock(WorkOrderQueryService.class);
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new CollectionsAnalyticsService(workOrderQueryService, clock);
    }

    @Test
    void snapshot_blankReceivable_isZeroOnlyWhenNotBillable_otherwiseExcludedAndWarned() {
        NormalizedWorkOrder notBillable = wo("A", "Not Billable", null, null, null, null);
        NormalizedWorkOrder unknownGap = wo("B", "Update Required", null, null, null, null);
        NormalizedWorkOrder known = wo("C", "Billed", new BigDecimal("5000"), null, TODAY.minusDays(20), null);

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(notBillable, unknownGap, known));

        CollectionsAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalReceivable()).isEqualByComparingTo("5000"); // only C; A contributes 0, B excluded
        assertThat(response.warnings()).anyMatch(w -> "MISSING_AMOUNT_RECEIVABLE".equals(w.code()) && w.affectedRecordCount() == 1);
    }

    @Test
    void snapshot_agedReceivables_sortedOldestFirst() {
        NormalizedWorkOrder recent = wo("A", "Billed", new BigDecimal("1000"), null, TODAY.minusDays(5), null);
        NormalizedWorkOrder old = wo("B", "Billed", new BigDecimal("2000"), null, TODAY.minusDays(60), null);
        NormalizedWorkOrder zeroReceivable = wo("C", "Billed", BigDecimal.ZERO, null, TODAY.minusDays(90), null);

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(recent, old, zeroReceivable));

        CollectionsAnalyticsResponse response = service.snapshot(null);

        assertThat(response.agedReceivables()).extracting(AgedReceivable::dealName).containsExactly("Deal B", "Deal A");
    }

    @Test
    void snapshot_totalCollected_excludesBlank() {
        NormalizedWorkOrder collected = wo("A", "Billed", null, new BigDecimal("300000"), null, null);
        NormalizedWorkOrder notCollected = wo("B", "Not Set", null, null, null, null);

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(collected, notCollected));

        CollectionsAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalCollected()).isEqualByComparingTo("300000");
    }

    private NormalizedWorkOrder wo(String id, String billingStatus, BigDecimal amountReceivable,
                                    BigDecimal collectedAmount, LocalDate probableEndDate, LocalDate dataDeliveryDate) {
        return new NormalizedWorkOrder(id, "Deal " + id, "WOCOMPANY_001", "OWNER_001", "Mining",
                "Not Set", probableEndDate, dataDeliveryDate, billingStatus, "Not Set",
                null, collectedAmount, amountReceivable, null, null);
    }
}
