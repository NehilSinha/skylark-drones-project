package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MondayItemDto(
        String id,
        String name,
        @JsonProperty("column_values") List<MondayColumnValueDto> columnValues
) {
}
