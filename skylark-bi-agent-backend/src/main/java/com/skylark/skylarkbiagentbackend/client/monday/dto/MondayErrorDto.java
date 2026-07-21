package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Monday.com can return HTTP 200 with a populated {@code errors} array (standard
 * GraphQL behavior) — this must always be checked even on an otherwise-successful
 * HTTP response. Fields beyond {@code message} vary by error type, so unknowns are
 * ignored rather than failing deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MondayErrorDto(String message, List<Object> path, Map<String, Object> extensions) {
}
