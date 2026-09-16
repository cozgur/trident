package dev.ozgurcetintas.trident.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point to Trident's configuration.
 *
 * <p>The configuration is resolved once, on first access, and cached for the life of the JVM.
 * There is deliberately no {@code reset()}, {@code reload()} or setter: configuration is an
 * input to a test run, not something a test may change underneath other tests.
 *
 * <p><strong>Overriding a single value.</strong> Use a system property
 * ({@code -Dweb.base.url=...}) or the mapped environment variable ({@code WEB_BASE_URL=...}).
 * Both outrank every file layer and leave the remaining shipped defaults intact.
 *
 * <p><strong>Overriding with a file.</strong> A consumer's own
 * {@code config/<env>.properties} is a different source path from
 * {@code config/default.properties}, so the two merge: keys it defines win and keys it omits
 * fall back to the shipped defaults.
 *
 * <p>Supplying a {@code config/default.properties} behaves differently. A consumer's copy sits
 * earlier on the classpath and <em>shadows</em> the shipped file rather than merging with it,
 * because {@code MERGE} combines the two distinct paths in {@code @Sources}, not two copies of
 * the same path. A partial file leaves the omitted keys unresolved, and reading one throws
 * {@link NullPointerException} rather than returning a default. A consumer replacing
 * {@code config/default.properties} must supply a complete file defining all five keys.
 *
 * <p>Resolution happens in two stages. Stage 1 decides the active environment:
 * {@code -Denv}, then the {@code ENV} environment variable, then {@code local}. Stage 2 builds
 * the configuration around that environment and is {@link ConfigLoader}'s job.
 */
public final class ConfigProvider {

    private static final String KEY_ENV = "env";
    private static final String ENVIRONMENT_VARIABLE_ENV = "ENV";
    private static final String DEFAULT_ENVIRONMENT = "local";

    private ConfigProvider() {}

    /**
     * Returns the configuration for the active environment, resolving it on first call.
     *
     * @return the cached configuration, never {@code null}
     */
    public static TridentConfig get() {
        return Holder.INSTANCE;
    }

    /**
     * Stage 1 of the bootstrap: decides which environment is active.
     *
     * <p>Package-private and pure so that the precedence can be unit-tested without touching the
     * cached instance. It holds no state of its own.
     *
     * @param systemProps system properties, keyed by property name
     * @param envVars environment variables, keyed by variable name
     * @return the value of the {@code env} system property, else the {@code ENV} environment
     *     variable, else {@code local}
     */
    static String resolveEnvironment(Map<String, String> systemProps, Map<String, String> envVars) {
        String fromSystemProperty = systemProps.get(KEY_ENV);
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty;
        }
        String fromEnvironmentVariable = envVars.get(ENVIRONMENT_VARIABLE_ENV);
        if (fromEnvironmentVariable != null && !fromEnvironmentVariable.isBlank()) {
            return fromEnvironmentVariable;
        }
        return DEFAULT_ENVIRONMENT;
    }

    private static Map<String, String> systemProperties() {
        Map<String, String> snapshot = new HashMap<>();
        for (String name : System.getProperties().stringPropertyNames()) {
            snapshot.put(name, System.getProperty(name));
        }
        return snapshot;
    }

    /**
     * Initialisation-on-demand holder. The instance is created when {@link #get()} is first
     * called and the field is final, so the cache is lazy and thread-safe without locking and
     * without any mutable static state.
     */
    private static final class Holder {

        private static final TridentConfig INSTANCE = create();

        private static TridentConfig create() {
            Map<String, String> systemProps = systemProperties();
            Map<String, String> envVars = System.getenv();
            return ConfigLoader.load(resolveEnvironment(systemProps, envVars), systemProps, envVars);
        }
    }
}
