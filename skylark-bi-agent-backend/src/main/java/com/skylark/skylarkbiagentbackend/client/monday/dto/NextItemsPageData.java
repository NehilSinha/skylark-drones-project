package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response shape of a pagination-continuation query: {@code next_items_page(cursor, limit) { ... } }. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NextItemsPageData(@JsonProperty("next_items_page") MondayItemsPageDto nextItemsPage) {
}
