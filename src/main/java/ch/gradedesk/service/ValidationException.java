package ch.gradedesk.service;

import java.util.Arrays;

/**
 * Fachlicher Eingabefehler.
 *
 * <p>Die Ausnahme trägt bewusst keinen fertigen Satz, sondern einen
 * Textschlüssel und seine Platzhalterwerte. So bleibt die Fachlogik frei von
 * Sprache und Darstellung, und das UI übersetzt die Meldung in die gerade
 * eingestellte Sprache.</p>
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final transient Object[] arguments;

    public ValidationException(String messageKey, Object... arguments) {
        super(messageKey + Arrays.toString(arguments));
        this.messageKey = messageKey;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    /** Der Schlüssel im Textbündel, z.B. {@code validation.grade.outOfRange}. */
    public String getMessageKey() {
        return messageKey;
    }

    /** Die Werte für die Platzhalter im Text. */
    public Object[] getArguments() {
        return arguments.clone();
    }
}
