package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Monday's cursor-pagination envelope, shared by both the initial and continuation query shapes. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MondayItemsPageDto(String cursor, List<MondayItemDto> items) {
}
