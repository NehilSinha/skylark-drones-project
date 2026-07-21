package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExecutionAnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);

    private WorkOrderQueryService workOrderQueryService;
    private ExecutionAnalyticsService service;

    @BeforeEach
    void setUp() {
        workOrderQueryService = Mockito.mock(WorkOrderQueryService.class);
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ExecutionAnalyticsService(workOrderQueryService, clock);
    }

    @Test
    void snapshot_delayDetection_excludesCompletedAndRecurringButFlagsOthers() {
        NormalizedWorkOrder overdueNotStarted = wo("A", "Not Started", TODAY.minusDays(10), null);
        NormalizedWorkOrder overdueCompleted = wo("B", "Completed", TODAY.minusDays(10), null);
        NormalizedWorkOrder overdueRecurring = wo("C", "Executed until current month", TODAY.minusDays(10), null);
        NormalizedWorkOrder notYetDue = wo("D", "Ongoing", TODAY.plusDays(10), null);
        NormalizedWorkOrder noEndDate = wo("E", "Ongoing", null, null);

        when(workOrderQueryService.queryAll(any())).thenReturn(
                List.of(overdueNotStarted, overdueCompleted, overdueRecurring, notYetDue, noEndDate));

        ExecutionAnalyticsResponse response = service.snapshot(null);

        assertThat(response.delayed()).hasSize(1);
        assertThat(response.delayed().get(0).dealName()).isEqualTo("Deal A");
        assertThat(response.delayed().get(0).daysOverdue()).isEqualTo(10);

        assertThat(response.statusDistribution())
                .containsEntry("Not Started", 1L)
                .containsEntry("Completed", 1L)
                .containsEntry("Executed until current month", 1L)
                .containsEntry("Ongoing", 2L);

        assertThat(response.warnings()).anyMatch(w -> "MISSING_PROBABLE_END_DATE".equals(w.code()) && w.affectedRecordCount() == 1);
    }

    @Test
    void snapshot_averageDeliveryVariance_onlyOverBothDatesPresent() {
        NormalizedWorkOrder onTime = wo("A", "Completed", TODAY.minusDays(5), TODAY.minusDays(5));
        NormalizedWorkOrder late = wo("B", "Completed", TODAY.minusDays(10), TODAY.minusDays(5));
        NormalizedWorkOrder noDeliveryDate = wo("C", "Ongoing", TODAY.minusDays(1), null);

        when(workOrderQueryService.queryAll(any())).thenReturn(List.of(onTime, late, noDeliveryDate));

        ExecutionAnalyticsResponse response = service.snapshot(null);

        assertThat(response.averageDeliveryVarianceDays()).isEqualTo(2.5); // (0 + 5) / 2
    }

    @Test
    void snapshot_noWorkOrders_returnsEmptyNotAnException() {
        when(workOrderQueryService.queryAll(any())).thenReturn(List.of());

        ExecutionAnalyticsResponse response = service.snapshot(null);

        assertThat(response.delayed()).isEmpty();
        assertThat(response.averageDeliveryVarianceDays()).isNull();
        assertThat(response.warnings()).isEmpty();
    }

    private NormalizedWorkOrder wo(String id, String executionStatus, LocalDate probableEndDate, LocalDate dataDeliveryDate) {
        return new NormalizedWorkOrder(id, "Deal " + id, "WOCOMPANY_001", "OWNER_001", "Mining",
                executionStatus, probableEndDate, dataDeliveryDate, "Not Set", "Not Set", null, null, null, null, null);
    }
}
