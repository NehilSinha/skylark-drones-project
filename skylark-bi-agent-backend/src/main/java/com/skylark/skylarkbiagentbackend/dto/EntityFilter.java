package com.skylark.skylarkbiagentbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The filter shape shared across analytics tools/services. All fields are optional
 * — an all-null instance means "no filter, everything in scope." Kept as a single
 * reusable shape (per ADR-001 / PHASE-4-DESIGN.md §0) rather than each tool
 * inventing its own filter parameters.
 *
 * <p>Every component is explicitly {@code @JsonProperty(required = false)}: Spring
 * AI's tool JSON-schema generator marks record components required by default, and
 * a strict tool-calling provider (Groq included) will reject a tool call that omits
 * any "required" property rather than treating a missing optional field as null —
 * discovered via the walking-skeleton live smoke test, see Phase 4 completion
 * report.
 */
public record EntityFilter(
        @JsonProperty(required = false) List<String> sectors,
        @JsonProperty(required = false) List<String> owners,
        @JsonProperty(required = false) List<String> clients,
        @JsonProperty(required = false) List<String> dealStatuses,
        @JsonProperty(required = false) DateRange dateRange
) {

    public static EntityFilter empty() {
        return new EntityFilter(null, null, null, null, null);
    }

    public boolean hasSectorFilter() {
        return sectors != null && !sectors.isEmpty();
    }

    public boolean hasOwnerFilter() {
        return owners != null && !owners.isEmpty();
    }

    public boolean hasClientFilter() {
        return clients != null && !clients.isEmpty();
    }

    public boolean hasStatusFilter() {
        return dealStatuses != null && !dealStatuses.isEmpty();
    }
}
