package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BillingAnalyticsServiceTest {

    private WorkOrderQueryService workOrderQueryService;
    private BillingAnalyticsService service;

    @BeforeEach
    void setUp() {
        workOrderQueryService = Mockito.mock(WorkOrderQueryService.class);
        service = new BillingAnalyticsService(workOrderQueryService);
    }

    @Test
    void snapshot_sumsAmountToBeBilled_excludingBlanks() {
        NormalizedWorkOrder billedA = wo("A", "Billed", new BigDecimal("100000"));
        NormalizedWorkOrder notSetB = wo("B", "Not Set", new BigDecimal("50000"));
        NormalizedWorkOrder missingAmount = wo("C", "Update Required", null);

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(billedA, notSetB, missingAmount));

        BillingAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalAmountToBeBilled()).isEqualByComparingTo("150000");
        assertThat(response.warnings()).anyMatch(w -> "MISSING_AMOUNT_TO_BE_BILLED".equals(w.code()) && w.affectedRecordCount() == 1);
    }

    @Test
    void snapshot_billingStatusDistribution_countsPerCanonicalStatus() {
        // WorkOrderNormalizer already collapses Billed/BIlled -> "Billed" before this
        // service ever sees the data, so both instances land in the same bucket.
        NormalizedWorkOrder a = wo("A", "Billed", new BigDecimal("1"));
        NormalizedWorkOrder b = wo("B", "Billed", new BigDecimal("1"));
        NormalizedWorkOrder c = wo("C", "Not Billable", new BigDecimal("1"));

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(a, b, c));

        BillingAnalyticsResponse response = service.snapshot(null);

        assertThat(response.billingStatusDistribution()).containsEntry("Billed", 2L).containsEntry("Not Billable", 1L);
    }

    @Test
    void snapshot_noWorkOrders_returnsZeroNotAnException() {
        when(workOrderQueryService.queryAll(any())).thenReturn(List.of());

        BillingAnalyticsResponse response = service.snapshot(null);

        assertThat(response.totalAmountToBeBilled()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.warnings()).isEmpty();
    }

    private NormalizedWorkOrder wo(String id, String billingStatus, BigDecimal amountToBeBilled) {
        return new NormalizedWorkOrder(id, "Deal " + id, "WOCOMPANY_001", "OWNER_001", "Mining",
                "Not Set", null, null, billingStatus, "Not Set", amountToBeBilled, null, null, null, null);
    }
}
