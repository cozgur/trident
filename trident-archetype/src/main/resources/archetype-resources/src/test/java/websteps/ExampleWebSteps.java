package ${package}.websteps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ozgurcetintas.trident.core.config.ConfigProvider;
import dev.ozgurcetintas.trident.web.DriverProvider;
import ${package}.pages.ExamplePage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the example browser scenario. Delete them once you have your own.
 *
 * <p><strong>No locator appears here, and none should.</strong> Steps say what a person does;
 * where the controls are is the page object's business. That boundary is what lets a markup
 * change touch one file instead of every scenario that walks through that screen.
 */
public class ExampleWebSteps {

    private ExamplePage page;

    @When("I open the example page")
    public void iOpenTheExamplePage() {
        page = new ExamplePage(DriverProvider.get(), ConfigProvider.get()).open();
    }

    @When("I enter the reference {string}")
    public void iEnterTheReference(String reference) {
        page.enterReference(reference);
    }

    @Then("the page is headed {string}")
    public void thePageIsHeaded(String expected) {
        assertThat(page.heading()).isEqualTo(expected);
    }
}
