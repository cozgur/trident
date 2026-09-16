package dev.ozgurcetintas.trident.runner;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

/**
 * Base class for a Cucumber suite driven by the JUnit Platform.
 *
 * <p>A consumer project subclasses this once per suite and says only which features it wants:
 *
 * <pre>{@code
 * @SelectClasspathResource("features")
 * public class CheckoutTestSuite extends TridentSuite {}
 * }</pre>
 *
 * <p>{@code @Suite} and {@code @IncludeEngines} are {@code @Inherited}, so the subclass needs
 * neither. It stays abstract here because an abstract class is not itself discovered as a
 * suite, which is what keeps this base from running with no features selected.
 *
 * <p><strong>Naming matters.</strong> Which plugin runs a suite is decided by its class name,
 * not by anything in this class:
 *
 * <ul>
 *   <li>{@code *TestSuite} — Surefire, the fast suite, no containers.
 *   <li>{@code *IT} — Failsafe, the integration suite.
 * </ul>
 *
 * <p>The tag expression each plugin passes is composed in the consumer's POM — Surefire sends
 * {@code @smoke and (...)} and Failsafe {@code @api and (...)} — so a suite cannot execute
 * another suite's scenarios whatever tag property is supplied. The reference implementation in
 * this repository shows the full plugin configuration.
 */
@Suite
@IncludeEngines("cucumber")
public abstract class TridentSuite {}
