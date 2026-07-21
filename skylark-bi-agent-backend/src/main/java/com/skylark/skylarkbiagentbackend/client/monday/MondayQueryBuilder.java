package com.skylark.skylarkbiagentbackend.client.monday;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds the two GraphQL query shapes Monday's item-pagination API requires: an
 * initial per-board query, then cursor-only continuation queries. Column selection
 * is deliberately generic ({@code column_values { id text value }}) so this builder
 * works for any board without knowing its schema in advance — see ADR-001
 * "Board abstraction".
 */
@Component
public class MondayQueryBuilder {

    private static final String ITEM_FIELDS = """
            id
            name
            column_values {
              id
              text
              value
            }
            """;

    public String initialItemsPageQuery() {
        return """
                query GetBoardItems($boardId: ID!, $limit: Int!) {
                  boards(ids: [$boardId]) {
                    items_page(limit: $limit) {
                      cursor
                      items {
                        %s
                      }
                    }
                  }
                }
                """.formatted(ITEM_FIELDS);
    }

    public Map<String, Object> initialItemsPageVariables(String boardId, int pageSize) {
        return Map.of("boardId", boardId, "limit", pageSize);
    }

    public String nextItemsPageQuery() {
        return """
                query GetNextItems($cursor: String!, $limit: Int!) {
                  next_items_page(cursor: $cursor, limit: $limit) {
                    cursor
                    items {
                      %s
                    }
                  }
                }
                """.formatted(ITEM_FIELDS);
    }

    public Map<String, Object> nextItemsPageVariables(String cursor, int pageSize) {
        return Map.of("cursor", cursor, "limit", pageSize);
    }
}
