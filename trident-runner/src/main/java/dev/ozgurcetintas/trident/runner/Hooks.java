package dev.ozgurcetintas.trident.runner;

import dev.ozgurcetintas.trident.core.context.ScenarioContext;
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
 * <p>The {@link ScenarioContext} is injected by Picocontainer, which creates one instance per
 * scenario and hands that same instance to every glue class in the scenario. Clearing it at
 * both ends is belt and braces: a fresh instance is already empty, and clearing afterwards
 * keeps a leaked reference from carrying data into anything that outlives the scenario.
 *
 * <p>The logger is an instance field rather than the customary {@code private static final}:
 * glue classes hold no static state of any kind.
 */
public class Hooks {

    private final Logger log = LoggerFactory.getLogger(Hooks.class);

    private final ScenarioContext context;

    public Hooks(ScenarioContext context) {
        this.context = context;
    }

    @Before
    public void before(Scenario scenario) {
        context.clear();
        log.info("Starting scenario: {}", scenario.getName());
    }

    @After
    public void after(Scenario scenario) {
        context.clear();
        log.info("Finished scenario: {} [{}]", scenario.getName(), scenario.getStatus());
    }
}
