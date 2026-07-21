package com.skylark.skylarkbiagentbackend.normalizer;

import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayColumnValueDto;
import com.skylark.skylarkbiagentbackend.client.monday.dto.MondayItemDto;

import java.util.HashMap;
import java.util.Map;

/** Shared by every board normalizer: column id -> raw text, board-schema-agnostic. */
public final class MondayColumnTextExtractor {

    private MondayColumnTextExtractor() {
    }

    public static Map<String, String> columnTextById(MondayItemDto item) {
        if (item.columnValues() == null) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (MondayColumnValueDto columnValue : item.columnValues()) {
            if (columnValue.id() != null) {
                map.put(columnValue.id(), columnValue.text());
            }
        }
        return map;
    }
}
