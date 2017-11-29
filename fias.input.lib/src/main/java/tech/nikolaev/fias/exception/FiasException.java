package tech.nikolaev.fias.exception;

/**
 * Created by andrey.l.nikolaev@mail.ru on 17.08.2017.
 */
public class FiasException extends Exception {


    public FiasException(Throwable cause) {
        super(cause);
    }

    public FiasException(String message) {
        super(message);
    }

    public FiasException(String message, Throwable cause) {
        super(message, cause);
    }
}
