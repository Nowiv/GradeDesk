package ch.notenrechner.service;

/**
 * Fachlicher Eingabefehler mit einer Meldung, die direkt im UI angezeigt werden kann.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
