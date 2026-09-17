package ${package}.steps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/**
 * Steps for the example scenario. Delete them once you have your own.
 *
 * <p>{@link ScenarioContext} arrives by constructor injection: Picocontainer builds this class
 * once per scenario and hands it the same context every other glue class in that scenario gets.
 * Nothing here is static, which is what lets scenarios run in any order.
 */
public class ExampleSteps {

    private final ScenarioContext context;

    public ExampleSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the configuration is loaded")
    public void theConfigurationIsLoaded() {
        // Composed here rather than read from a file, so that finding it in the context later
        // is evidence this method actually ran.
        context.put(
                "resolvedConfiguration",
                ConfigProvider.get().env() + "|" + ConfigProvider.get().apiBaseUrl());
    }

    @Then("the environment is {string}")
    public void theEnvironmentIs(String expected) {
        assertThat(ConfigProvider.get().env()).isEqualTo(expected);
    }

    @Then("the scenario context still holds {string}")
    public void theScenarioContextStillHolds(String expected) {
        // Throws if the earlier step never ran: ScenarioContext.get rejects a missing key
        // rather than returning null.
        assertThat(context.get("resolvedConfiguration", String.class)).isEqualTo(expected);
    }
}
