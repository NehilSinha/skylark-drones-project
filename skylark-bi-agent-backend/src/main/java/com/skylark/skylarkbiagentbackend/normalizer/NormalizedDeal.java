package com.skylark.skylarkbiagentbackend.normalizer;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A Deal Tracker board item after normalization. Every field except {@code itemId}
 * and {@code dealName} may be {@code null} — that is the normalizer's honest
 * representation of "this field was blank or unparseable on the source row," which
 * analytics services must handle explicitly (exclude + warn), never coerce to a
 * default.
 */
public record NormalizedDeal(
        String itemId,
        String dealName,
        String ownerCode,
        String clientCode,
        DealStatus status,
        LocalDate closeDate,
        ClosureProbability closureProbability,
        BigDecimal dealValue,
        LocalDate tentativeCloseDate,
        String dealStage,
        String productDeal,
        String sector,
        LocalDate createdDate
) {
}
