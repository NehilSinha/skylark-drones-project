package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Response shape of the first-page query: {@code boards(ids: [...]) { items_page { ... } } }. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoardsItemsPageData(List<BoardWrapper> boards) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BoardWrapper(@JsonProperty("items_page") MondayItemsPageDto itemsPage) {
    }
}
