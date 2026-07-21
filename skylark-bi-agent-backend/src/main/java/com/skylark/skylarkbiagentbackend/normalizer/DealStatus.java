package com.skylark.skylarkbiagentbackend.normalizer;

/** Canonical Deal Status values. {@code UNKNOWN} covers blank or unrecognized text — never guessed. */
public enum DealStatus {
    OPEN, WON, DEAD, ON_HOLD, UNKNOWN
}
