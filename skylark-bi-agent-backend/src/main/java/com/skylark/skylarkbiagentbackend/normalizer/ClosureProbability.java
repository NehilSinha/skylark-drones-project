package com.skylark.skylarkbiagentbackend.normalizer;

/**
 * Deal Stage closure confidence. Deliberately has no {@code UNKNOWN} member —
 * unrecognized/blank text maps to a {@code null} reference on {@code NormalizedDeal}
 * instead, since "probability not set" must be excludable from weighted math, not
 * just another enum branch to handle.
 */
public enum ClosureProbability {
    HIGH, MEDIUM, LOW
}
