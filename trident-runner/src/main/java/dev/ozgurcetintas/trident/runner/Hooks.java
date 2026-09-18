package dev.ozgurcetintas.trident.runner;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scenario lifecycle hooks.
 *
 * <p>Published as part of the framework: a consumer project puts
 * {@code dev.ozgurcetintas.trident.runner} on its Cucumber glue path and gets these hooks
 * without writing them. They know nothing about any application under test.
 *
 * <p>These hooks do not touch the scenario context. They used to clear it at both ends,
 * as belt and braces, and that was a defect: Cucumber does not order hooks between glue
 * packages, so the framework's clear could run after a consumer's {@code @Before} and delete
 * the fixture it had just stored, or before their {@code @After} and delete what it needed to
 * clean up. Picocontainer already builds one context per scenario and discards it afterwards,
 * so there was nothing to clear — only someone else's data to lose.
 *
 * <p>The logger is an instance field rather than the customary {@code private static final}:
 * glue classes hold no static state of any kind.
 */
public class Hooks {

    private final Logger log = LoggerFactory.getLogger(Hooks.class);

    @Before
    public void before(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());
    }

    @After
    public void after(Scenario scenario) {
        log.info("Finished scenario: {} [{}]", scenario.getName(), scenario.getStatus());
    }
}
