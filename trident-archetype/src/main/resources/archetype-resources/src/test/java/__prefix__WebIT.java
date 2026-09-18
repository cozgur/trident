package ${package};

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The browser suite. Failsafe picks it up by the {@code *IT} name and filters it to
 * {@code @web}, so it runs only under {@code -Pweb}.
 *
 * <p>It is a third suite rather than another tag on {@link ${prefix}IT} because its
 * preconditions differ: it needs a browser, and a machine without one must still be able to run
 * everything else. It also keeps the two layers separable in a report and in CI, since each
 * suite writes its own message log.
 *
 * <p>Nothing here runs by default. {@code mvn verify} activates the smoke profile, which skips
 * this suite entirely, so a freshly generated project is green on a machine with no browser.
 */
@SelectClasspathResource("features")
public class ${prefix}WebIT extends TridentSuite {}
