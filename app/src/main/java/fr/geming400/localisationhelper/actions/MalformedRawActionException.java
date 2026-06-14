package fr.geming400.localisationhelper.actions;

public final class MalformedRawActionException extends RuntimeException {
    public MalformedRawActionException(String message, BaseAction<?, ?> action, String rawContent) {
        super(message + ". Parsing done for action " + action + " with raw malformed action content '" + rawContent + "'.");
    }

    public MalformedRawActionException(String message, Throwable cause, BaseAction<?, ?> action, String rawContent) {
        super(message + ". Parsing done for action " + action + " with raw malformed action content '" + rawContent + "'.", cause);
    }
}
