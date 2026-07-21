package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.config.WorkOrderColumnMapping;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedWorkOrder;
import com.skylark.skylarkbiagentbackend.normalizer.WorkOrderNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fetches, normalizes, and filters Work Order Tracker items — the Work Order board
 * counterpart of {@link DealQueryService}, same responsibilities and same shape.
 */
@Component
public class WorkOrderQueryService {

    private final MondayGraphQLClient mondayClient;
    private final MondayProperties mondayProperties;
    private final WorkOrderNormalizer normalizer;
    private final WorkOrderColumnMapping columnMapping;

    public WorkOrderQueryService(MondayGraphQLClient mondayClient,
                                  MondayProperties mondayProperties,
                                  WorkOrderNormalizer normalizer,
                                  WorkOrderColumnMapping columnMapping) {
        this.mondayClient = mondayClient;
        this.mondayProperties = mondayProperties;
        this.normalizer = normalizer;
        this.columnMapping = columnMapping;
    }

    public List<NormalizedWorkOrder> queryAll(EntityFilter filter) {
        List<MondayItemDto> items = mondayClient.fetchAllBoardItems(mondayProperties.boards().workOrdersId());

        return items.stream()
                .filter(normalizer::isValidRecord)
                .map(item -> normalizer.normalize(item, columnMapping))
                .filter(workOrder -> matches(workOrder, filter))
                .toList();
    }

    private boolean matches(NormalizedWorkOrder workOrder, EntityFilter filter) {
        if (filter == null) {
            return true;
        }
        if (filter.hasSectorFilter() && !containsIgnoreCase(filter.sectors(), workOrder.sector())) {
            return false;
        }
        if (filter.hasOwnerFilter() && !containsIgnoreCase(filter.owners(), workOrder.ownerCode())) {
            return false;
        }
        if (filter.hasClientFilter() && !containsIgnoreCase(filter.clients(), workOrder.customerCode())) {
            return false;
        }
        // dealStatuses has no equivalent on the Work Order board (no "deal status"
        // concept here) — that filter dimension is silently a no-op for this board,
        // consistent with RevenueAnalyticsService stripping fields that don't apply.
        if (filter.dateRange() != null) {
            if (workOrder.probableEndDate() == null) {
                return false;
            }
            if (workOrder.probableEndDate().isBefore(filter.dateRange().start())
                    || workOrder.probableEndDate().isAfter(filter.dateRange().end())) {
                return false;
            }
        }
        return true;
    }

    private boolean containsIgnoreCase(List<String> candidates, String value) {
        if (value == null) {
            return false;
        }
        return candidates.stream().anyMatch(value::equalsIgnoreCase);
    }
}
