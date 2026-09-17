package ${package};

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The integration suite. Failsafe picks it up by the {@code *IT} name and filters it to
 * {@code @api}, so it runs only under {@code -Papi}.
 *
 * <p>It starts empty: there are no {@code @api} scenarios yet, and Failsafe is skipped unless
 * you ask for it. Write a scenario tagged {@code @api} when you have a target to point at, and
 * put anything that needs starting - a container, a client - in a glue package listed only on
 * this suite's {@code cucumber.glue} in the POM.
 */
@SelectClasspathResource("features")
public class ${prefix}IT extends TridentSuite {}
