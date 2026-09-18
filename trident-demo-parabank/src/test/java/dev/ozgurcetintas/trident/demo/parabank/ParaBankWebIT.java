package dev.ozgurcetintas.trident.demo.parabank;

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The browser suite. Failsafe picks it up by the {@code *IT} name and filters it to
 * {@code @web}, so it runs neither {@code @smoke} nor {@code @api} scenarios.
 *
 * <p>It is a third suite rather than a second tag on {@link ParaBankIT} because its
 * preconditions and its evidence differ. It needs a browser as well as a container, and the
 * execution gate reads one message log per suite — folded together, a failure would say the
 * integration suite broke without saying which layer did.
 */
@SelectClasspathResource("features")
public class ParaBankWebIT extends TridentSuite {}
