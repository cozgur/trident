package dev.ozgurcetintas.trident.runner;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.core.context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/**
 * Steps for the wiring scenario.
 *
 * <p>{@link ScenarioContext} arrives by constructor injection: Picocontainer builds this class
 * once per scenario and supplies the same context instance it gives to {@link Hooks}. Nothing
 * here is static, so two scenarios can never see each other's data.
 */
public class ConfigurationSteps {

    private final ScenarioContext context;

    public ConfigurationSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the framework configuration is loaded")
    public void theFrameworkConfigurationIsLoaded() {
        TridentConfig config = ConfigProvider.get();
        context.put("webBaseUrl", config.webBaseUrl());
    }

    @Then("the web base URL is {string}")
    public void theWebBaseUrlIs(String expected) {
        assertThat(ConfigProvider.get().webBaseUrl()).isEqualTo(expected);
    }

    @Then("the default timeout is {long} seconds")
    public void theDefaultTimeoutIs(long expected) {
        assertThat(ConfigProvider.get().defaultTimeoutSeconds()).isEqualTo(expected);
    }

    @Then("the web base URL recorded earlier is readable from the scenario context")
    public void theWebBaseUrlRecordedEarlierIsReadable() {
        assertThat(context.get("webBaseUrl", String.class)).isEqualTo("http://localhost");
    }
}
