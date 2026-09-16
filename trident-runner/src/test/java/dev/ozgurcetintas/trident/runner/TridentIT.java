package dev.ozgurcetintas.trident.runner;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Entry point for the container-backed integration suite.
 *
 * <p>Runs under Failsafe, which matches {@code **}{@code /*IT.java}. It selects the same
 * feature tree as {@link TridentTestSuite}; the two are kept apart by tag, not by directory.
 * Failsafe composes its filter as {@code @api and (...)} and Surefire as
 * {@code @smoke and (...)}, so neither suite can execute the other's scenarios.
 *
 * <p>This suite requires Docker. {@link TridentTestSuite} deliberately does not, so the
 * README quickstart stays runnable with Git and Java 21 alone.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class TridentIT {}
