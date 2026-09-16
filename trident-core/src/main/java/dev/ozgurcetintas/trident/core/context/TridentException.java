package dev.ozgurcetintas.trident.core.context;

/**
 * Trident's base unchecked exception.
 *
 * <p>Framework failures are unchecked: a step definition cannot meaningfully recover from a
 * misconfigured framework, and forcing a checked exception through every step signature would
 * add handling code that only ever rethrows.
 */
public class TridentException extends RuntimeException {

    public TridentException(String message) {
        super(message);
    }

    public TridentException(String message, Throwable cause) {
        super(message, cause);
    }
}
