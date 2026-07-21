package com.skylark.skylarkbiagentbackend.resolution;

/** Confidence tier of a cross-board entity match. See ADR-001 "Entity Resolution Strategy." */
public enum MatchConfidence {
    /** Exact normalized client-code match. */
    HIGH,
    /** Fuzzy deal-name match at or above the auto-accept threshold. */
    MEDIUM,
    /** Fuzzy deal-name match above the possible-match threshold but below auto-accept. */
    LOW,
    /** No match found on either side. */
    UNMATCHED
}
