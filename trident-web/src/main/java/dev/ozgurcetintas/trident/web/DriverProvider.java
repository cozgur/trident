package dev.ozgurcetintas.trident.web;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.TridentException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-scoped access to the browser, with a lifecycle the caller states explicitly.
 *
 * <p>A scenario starts a driver, uses it, and quits it:
 *
 * <pre>{@code
 * @Before("@web")
 * public void startBrowser() {
 *     DriverProvider.start(ConfigProvider.get());
 * }
 *
 * @After("@web")
 * public void stopBrowser() {
 *     DriverProvider.quit();
 * }
 * }</pre>
 *
 * <p><strong>Why a ThreadLocal, in a codebase with no static mutable state.</strong> This is
 * the one exception, and it is narrow on purpose. Cucumber runs each scenario on a thread and,
 * once parallel execution is enabled, several at once; a driver in a plain static field would
 * be shared between them and a driver in an injected per-scenario object would have to be
 * threaded through every page object by hand. A {@code ThreadLocal} is per-scenario state that
 * happens to be reachable statically. It is confined to this class, it holds one reference, and
 * {@link #quit()} removes it — which matters because Cucumber reuses pooled threads, and a
 * {@code ThreadLocal} that is only overwritten and never removed keeps a dead browser reachable
 * for the life of the pool.
 *
 * <p><strong>No implicit creation.</strong> {@link #get()} on a thread that never called
 * {@link #start} throws rather than quietly opening a browser. A provider that creates on first
 * use will, sooner or later, open a second browser because a hook did not run, and the symptom
 * is a scenario that passes against a page nobody is looking at.
 */
public final class DriverProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DriverProvider.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverProvider() {}

    /**
     * Opens a browser for the current thread.
     *
     * @param config the configuration the driver is built from
     * @throws TridentException if this thread already has a driver, which means a previous
     *     scenario did not quit its own
     */
    public static void start(TridentConfig config) {
        if (DRIVER.get() != null) {
            throw new TridentException("This thread already has a driver. A scenario started a "
                    + "browser without quitting the previous one; check that the @After hook "
                    + "calling DriverProvider.quit() is on the glue path for every @web scenario.");
        }
        DRIVER.set(WebDriverFactory.create(config));
        LOG.debug("Driver started on thread {}", Thread.currentThread().getName());
    }

    /**
     * The browser belonging to the current thread.
     *
     * @return the driver, never {@code null}
     * @throws TridentException if this thread has no driver
     */
    public static WebDriver get() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new TridentException("No driver on this thread. Call DriverProvider.start(config) "
                    + "first — a @Before(\"@web\") hook is the usual place. Trident does not open a "
                    + "browser implicitly.");
        }
        return driver;
    }

    /** Whether the current thread has a driver. Lets a hook be written without catching. */
    public static boolean isStarted() {
        return DRIVER.get() != null;
    }

    /**
     * Closes this thread's browser and forgets it. Does nothing if there is none, so an
     * {@code @After} hook can run unconditionally after a scenario that failed before starting.
     */
    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } finally {
            // In a finally block: if quit() throws because the browser already died, the
            // reference must still go, or the next scenario on this thread is refused a driver
            // by start() and reports the wrong failure.
            DRIVER.remove();
            LOG.debug("Driver quit on thread {}", Thread.currentThread().getName());
        }
    }
}
