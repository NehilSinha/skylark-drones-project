package com.skylark.skylarkbiagentbackend.client.monday.dto;

import java.util.Map;

/** Standard GraphQL-over-HTTP request body: a query document plus its variables. */
public record MondayGraphQlRequest(String query, Map<String, Object> variables) {
}
