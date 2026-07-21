package com.skylark.skylarkbiagentbackend.exception;

/** No conversation memory document exists for the given session id. */
public class ConversationNotFoundException extends BiAgentException {

    public ConversationNotFoundException(String sessionId) {
        super(ErrorCode.CONVERSATION_NOT_FOUND, "No conversation found for session: " + sessionId);
    }
}
