package com.skylark.skylarkbiagentbackend.resolution;

/**
 * One resolved (or deliberately unresolved) pairing between a Deal-board client and
 * a Work-Order-board client. Either code may be {@code null} when only one side has
 * a record — that is a real, reportable outcome (see {@link MatchConfidence#UNMATCHED}),
 * never dropped silently.
 */
public record ResolvedEntity(
        String normalizedClientKey,
        String dealClientCode,
        String workOrderClientCode,
        MatchConfidence confidence
) {
}
