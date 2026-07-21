package com.skylark.skylarkbiagentbackend.dto;

import java.time.LocalDate;

/**
 * A resolved, concrete date window. Once a value of this type exists, no code
 * downstream ever interprets a phrase like "this quarter" again — that resolution
 * happens exactly once, in {@code DateExpressionResolver}.
 */
public record DateRange(LocalDate start, LocalDate end, String label) {
}
