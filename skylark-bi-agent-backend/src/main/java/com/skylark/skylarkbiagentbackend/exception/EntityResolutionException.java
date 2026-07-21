package com.skylark.skylarkbiagentbackend.exception;

/**
 * Signals that the entity-resolution pipeline itself is misconfigured or broken
 * (e.g. an invalid prefix-mapping configuration). This is NOT thrown for ordinary
 * unmatched records during normal operation — those are represented as a
 * {@code DataQualityWarning} attached to an otherwise-successful response, since a
 * client-code/deal-name mismatch is expected, everyday data, not a system failure.
 */
public class EntityResolutionException extends BiAgentException {

    public EntityResolutionException(String message) {
        super(ErrorCode.ENTITY_RESOLUTION_ERROR, message);
    }
}
