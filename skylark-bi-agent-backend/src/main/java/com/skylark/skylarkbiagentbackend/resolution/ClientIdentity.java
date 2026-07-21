package com.skylark.skylarkbiagentbackend.resolution;

/**
 * The minimal identity-relevant projection of a board record used for cross-board
 * matching — deliberately independent of {@code NormalizedDeal}/a future
 * {@code NormalizedWorkOrder}, so entity resolution doesn't require both boards'
 * full normalization pipelines to exist before it can be built and tested.
 */
public record ClientIdentity(String code, String name, String sourceBoard) {
}
