package dev.ozgurcetintas.trident.demo.parabank;

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The container-backed suite. Failsafe picks it up by the {@code *IT} name and filters it to
 * {@code @api}. This suite requires Docker; {@link ParaBankTestSuite} deliberately does not.
 */
@SelectClasspathResource("features")
public class ParaBankIT extends TridentSuite {}
