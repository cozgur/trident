package dev.ozgurcetintas.trident.runner;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Entry point for the Cucumber suite.
 *
 * <p>The scenarios run through the JUnit Platform's Cucumber engine, so they are discovered
 * and reported by the same machinery as any other JUnit test. Glue path, reporting plugins
 * and parallelism live in {@code junit-platform.properties}; the tag filter arrives as a
 * system property that Surefire forwards from the active Maven profile.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class TridentTestSuite {}
