package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One column's value on one item, in Monday's generic shape. {@code text} is the
 * human-readable rendering; {@code value} is the raw JSON-encoded column payload
 * (shape differs per column type: status, date, numbers, etc). Column-type-specific
 * parsing belongs in the {@code normalizer} package, not here — this client stays
 * board-schema-agnostic.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MondayColumnValueDto(String id, String text, String value) {
}
