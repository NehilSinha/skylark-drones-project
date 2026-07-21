package com.skylark.skylarkbiagentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps logical Deal Tracker fields to Monday column IDs. Column IDs are assigned by
 * Monday when a column is created and cannot be predicted — every field here
 * defaults to blank and must be set once the real board exists
 * ({@code MONDAY_DEALS_COLUMN_*} env vars). A blank mapping is safe: the normalizer
 * treats an unmapped field as always-absent (null) rather than failing, which is
 * exactly what lets this run today against WireMock/test fixtures with made-up
 * column IDs and, unchanged, against the real board once it's provisioned.
 */
@ConfigurationProperties(prefix = "monday.column-mapping.deals")
public record DealColumnMapping(
        String ownerCode,
        String clientCode,
        String dealStatus,
        String closeDate,
        String closureProbability,
        String dealValue,
        String tentativeCloseDate,
        String dealStage,
        String productDeal,
        String sector,
        String createdDate
) {
}
