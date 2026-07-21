package com.skylark.skylarkbiagentbackend.analytics;

import com.skylark.skylarkbiagentbackend.client.monday.MondayGraphQLClient;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;
import com.skylark.skylarkbiagentbackend.config.DealColumnMapping;
import com.skylark.skylarkbiagentbackend.config.MondayProperties;
import com.skylark.skylarkbiagentbackend.dto.EntityFilter;
import com.skylark.skylarkbiagentbackend.normalizer.DealNormalizer;
import com.skylark.skylarkbiagentbackend.normalizer.NormalizedDeal;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fetches, normalizes, and filters Deal Tracker items — no aggregation. Every
 * analytics service that reads the Deal board goes through this rather than
 * calling {@link MondayGraphQLClient} directly, so fetch/normalize/filter logic
 * exists in exactly one place.
 */
@Component
public class DealQueryService {

    private final MondayGraphQLClient mondayClient;
    private final MondayProperties mondayProperties;
    private final DealNormalizer normalizer;
    private final DealColumnMapping columnMapping;

    public DealQueryService(MondayGraphQLClient mondayClient,
                             MondayProperties mondayProperties,
                             DealNormalizer normalizer,
                             DealColumnMapping columnMapping) {
        this.mondayClient = mondayClient;
        this.mondayProperties = mondayProperties;
        this.normalizer = normalizer;
        this.columnMapping = columnMapping;
    }

    public List<NormalizedDeal> queryAll(EntityFilter filter) {
        List<MondayItemDto> items = mondayClient.fetchAllBoardItems(mondayProperties.boards().dealsId());

        return items.stream()
                .filter(normalizer::isValidRecord)
                .map(item -> normalizer.normalize(item, columnMapping))
                .filter(deal -> matches(deal, filter))
                .toList();
    }

    private boolean matches(NormalizedDeal deal, EntityFilter filter) {
        if (filter == null) {
            return true;
        }
        if (filter.hasSectorFilter() && !containsIgnoreCase(filter.sectors(), deal.sector())) {
            return false;
        }
        if (filter.hasOwnerFilter() && !containsIgnoreCase(filter.owners(), deal.ownerCode())) {
            return false;
        }
        if (filter.hasClientFilter() && !containsIgnoreCase(filter.clients(), deal.clientCode())) {
            return false;
        }
        if (filter.hasStatusFilter() && (deal.status() == null || !containsIgnoreCase(filter.dealStatuses(), deal.status().name()))) {
            return false;
        }
        if (filter.dateRange() != null) {
            // Pipeline questions are almost always about when a deal is expected to
            // close, not when it was created, so the date range matches against
            // tentativeCloseDate. A deal with no tentative close date is excluded
            // from a date-scoped query rather than ambiguously included.
            if (deal.tentativeCloseDate() == null) {
                return false;
            }
            if (deal.tentativeCloseDate().isBefore(filter.dateRange().start())
                    || deal.tentativeCloseDate().isAfter(filter.dateRange().end())) {
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
