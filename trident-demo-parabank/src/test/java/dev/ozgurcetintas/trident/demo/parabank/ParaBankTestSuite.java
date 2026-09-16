package dev.ozgurcetintas.trident.demo.parabank;

import dev.ozgurcetintas.trident.runner.TridentSuite;
import org.junit.platform.suite.api.SelectClasspathResource;

/**
 * The fast suite. Surefire picks it up by the {@code *TestSuite} name and filters it to
 * {@code @smoke}, so it runs without Docker.
 *
 * <p>{@code @Suite} and {@code @IncludeEngines("cucumber")} are inherited from
 * {@link TridentSuite}; all this class decides is which features to select.
 */
@SelectClasspathResource("features")
public class ParaBankTestSuite extends TridentSuite {}
