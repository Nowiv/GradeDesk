package ch.gradedesk.persistence;

/** Fehler beim Lesen oder Schreiben des Datenbestands. */
public class RepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryException(String message) {
        super(message);
    }
}
