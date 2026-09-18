package ${package}.websteps;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.web.DriverProvider;
import io.cucumber.java.After;
import io.cucumber.java.Before;

/**
 * Opens a browser for each {@code @web} scenario and closes it afterwards.
 *
 * <p>Scope is the scenario, not the suite. A browser holds cookies, storage and a session, so
 * sharing one would hand the next scenario this one's login — the coupling that scenario-owned
 * data exists to prevent.
 *
 * <p>This class lives in the web glue package and nowhere else. Cucumber runs hooks for every
 * glue package a suite loads, so a browser hook on the shared {@code steps} path would open a
 * browser for the api suite too.
 */
public class BrowserLifecycle {

    @Before("@web")
    public void startBrowser() {
        DriverProvider.start(ConfigProvider.get());
    }

    /**
     * {@code quit()} is a no-op when this thread has no driver, so this needs no guard.
     *
     * <p>{@code order = 1} on purpose. Cucumber runs {@code @After} hooks highest order first,
     * so anything that wants to look at the browser before it closes — a screenshot on failure,
     * say — gives itself a higher number. Left at Cucumber's default of 10000, this would close
     * the browser before any of them ran, and the only symptom would be a report with no
     * screenshots in it.
     */
    @After(value = "@web", order = 1)
    public void stopBrowser() {
        DriverProvider.quit();
    }
}
