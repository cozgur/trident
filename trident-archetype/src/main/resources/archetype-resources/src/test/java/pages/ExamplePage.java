package ${package}.pages;

import static org.openqa.selenium.support.locators.RelativeLocator.with;

import dev.ozgurcetintas.trident.core.config.TridentConfig;
import dev.ozgurcetintas.trident.web.LoadablePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * An example page object. Replace it with one for your own application.
 *
 * <p>It renders its own page from a data URL rather than pointing at a server, so the generated
 * project has one {@code @web} scenario that passes the moment you run it — the browser
 * equivalent of the smoke scenario. If it passes, your browser and driver are set up; if it
 * fails, the problem is your machine, not your application. Point {@link #open()} at
 * {@code config.webBaseUrl()} once you have something to point it at.
 *
 * <p><strong>The locator ladder.</strong> Trident's reference suite drives an application it is
 * not allowed to modify, and the strategy that came out of it is recorded in
 * {@code docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md} in the Trident repository.
 * First rung that fits wins:
 *
 * <ol>
 *   <li>Accessible name — a label, an {@code aria-label}, the text on a button.
 *   <li>Relative locator — {@code with(tagName("input")).below(label)}, which is what rescues a
 *       form whose labels have no {@code for} attribute.
 *   <li>Structural XPath from an anchor you already know, never from the document root.
 *   <li>CSS or id, only where the attribute is genuinely stable.
 * </ol>
 *
 * <p>Two things are banned outright: an absolute XPath (a locator starting {@code //}) and a
 * positional index ({@code [2]}, {@code [last()]}). Both pin a position rather than a thing.
 * Trident's reference suite enforces that with a test that reads its page-object sources; copy
 * it if the rule matters to you.
 */
public final class ExamplePage extends LoadablePage {

    /**
     * A page with the shapes worth demonstrating: a button whose accessible name is its own
     * text, and an input whose label is not associated with it, which is the case rung 2 exists
     * for. Replace the whole thing with your application's URL.
     */
    private static final String PAGE = "data:text/html,"
            + "<html><body>"
            + "<h1>Trident</h1>"
            + "<p><b>Reference</b></p><input type='text' name='reference'>"
            + "<button type='button' onclick='document.title=\"clicked\"'>Continue</button>"
            + "</body></html>";

    /** Rung 1: the words the user reads on the button are its accessible name. */
    private static final By CONTINUE = By.xpath(".//button[normalize-space()='Continue']");

    /** Rung 1 again, used only as an anchor for the relative locator below. */
    private static final By REFERENCE_LABEL = By.xpath(".//b[normalize-space()='Reference']");

    private static final By HEADING = By.tagName("h1");

    public ExamplePage(WebDriver driver, TridentConfig config) {
        super(driver, config);
    }

    /**
     * The condition that says this page has finished rendering.
     *
     * <p>Pick one that is false on a half-rendered page. "The element exists" is often true
     * immediately and proves nothing; "the table has rows", "the dropdown has options", "the
     * result panel is visible" are the useful shapes.
     */
    @Override
    protected ExpectedCondition<?> loadedCondition() {
        return ExpectedConditions.visibilityOfElementLocated(CONTINUE);
    }

    @Override
    protected String pageName() {
        return "Example";
    }

    public ExamplePage open() {
        openAt(PAGE);
        return this;
    }

    /** @return the heading text the page shows */
    public String heading() {
        return visible(HEADING).getText();
    }

    /**
     * Rung 2: the input is found relative to the label above it. {@code below} orders its
     * matches by proximity to the anchor, so this is the nearest input under "Reference" even
     * though nothing in the markup connects them.
     *
     * @param reference the text to type
     */
    public ExamplePage enterReference(String reference) {
        visible(with(By.tagName("input")).below(anchor(REFERENCE_LABEL))).sendKeys(reference);
        return this;
    }

    public void continueOn() {
        clickable(CONTINUE).click();
    }
}
