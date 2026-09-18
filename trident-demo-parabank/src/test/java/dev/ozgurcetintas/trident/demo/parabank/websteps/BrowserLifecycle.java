package dev.ozgurcetintas.trident.demo.parabank.websteps;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.web.DriverProvider;
import io.cucumber.java.After;
import io.cucumber.java.Before;

/**
 * Opens a browser for each {@code @web} scenario and closes it afterwards.
 *
 * <p>Scope is the scenario, not the suite — the opposite of the container, and for the opposite
 * reason. A container takes seconds to start and holds no per-test state; a browser starts in
 * well under a second and holds cookies, storage and a session. Sharing one between scenarios
 * would hand the second scenario the first one's login, which is the same coupling
 * scenario-owned data exists to prevent.
 *
 * <p>This class lives in the web glue package and nowhere else. Cucumber's hooks run for every
 * glue package a suite loads, so a browser hook on the shared steps path would open a browser
 * for the API suite as well — the same defect that once started a container for the
 * Docker-free smoke run, recorded in ADR 0010.
 */
public class BrowserLifecycle {

    @Before("@web")
    public void startBrowser() {
        DriverProvider.start(ConfigProvider.get());
    }

    /**
     * Runs even when the scenario failed before opening anything: {@code quit()} is a no-op
     * when this thread has no driver, so the hook needs no guard of its own.
     */
    @After("@web")
    public void stopBrowser() {
        DriverProvider.quit();
    }
}
