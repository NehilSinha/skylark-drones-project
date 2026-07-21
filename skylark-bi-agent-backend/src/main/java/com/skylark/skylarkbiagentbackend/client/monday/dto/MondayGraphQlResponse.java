package com.skylark.skylarkbiagentbackend.client.monday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MondayGraphQlResponse<T>(T data, List<MondayErrorDto> errors) {

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
