package dev.ozgurcetintas.trident.demo.parabank.fixtures;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Holds the HTTP exchange of the scenario running on this thread, in memory.
 *
 * <p>Nothing is written anywhere and nothing is attached to a report unless the scenario fails.
 * A green run produces no artefacts at all, which is the point: a report full of successful
 * requests is a report nobody opens.
 *
 * <p>Thread-scoped for the same reason the driver is. Scenarios do not share it, and it is
 * cleared after every scenario whatever the outcome, so one scenario's exchange can never be
 * attached to another's failure.
 */
public final class ApiExchangeLog {

    private static final ThreadLocal<ByteArrayOutputStream> BUFFER =
            ThreadLocal.withInitial(ByteArrayOutputStream::new);

    private ApiExchangeLog() {}

    /** The stream REST Assured's logging filters write to. */
    public static PrintStream stream() {
        return new PrintStream(BUFFER.get(), true, StandardCharsets.UTF_8);
    }

    /** Everything this scenario has sent and received so far. */
    public static String text() {
        return BUFFER.get().toString(StandardCharsets.UTF_8);
    }

    /** Forgets this thread's exchange. Called after every scenario, passed or failed. */
    public static void clear() {
        BUFFER.remove();
    }
}
