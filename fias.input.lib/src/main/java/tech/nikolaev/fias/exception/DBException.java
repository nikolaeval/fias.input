package tech.nikolaev.fias.exception;

public class DBException extends FiasException {


    public DBException(Throwable cause) {
        super(cause);
    }

    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}
