package io.github.vaibhavkatyal.javask;

/** Error returned by Groq or encountered while contacting it. */
public final class GroqException extends RuntimeException {
    private final int statusCode;

    GroqException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    GroqException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /** HTTP status, or -1 for a network or response parsing failure. */
    public int getStatusCode() {
        return statusCode;
    }
}
