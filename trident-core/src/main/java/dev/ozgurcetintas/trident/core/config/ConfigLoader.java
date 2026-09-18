package dev.ozgurcetintas.trident.core.config;

import java.util.HashMap;
import java.util.Map;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Factory;

/**
 * Builds a {@link TridentConfig} from an already-resolved environment name.
 *
 * <p>This is stage 2 of Trident's two-stage bootstrap. Stage 1 — deciding which environment
 * is active — belongs to {@link ConfigProvider}; this class applies no fallback of its own
 * and takes the environment it is given.
 *
 * <p>Precedence, highest first: system properties, environment variables,
 * {@code classpath:config/${env}.properties}, {@code classpath:config/default.properties}.
 * The first two arrive as imported maps, which Owner ranks above anything in
 * {@code @Sources}; the last two are the {@code @Sources} chain itself.
 */
final class ConfigLoader {

    private static final String KEY_ENV = "env";

    /**
     * The closed mapping from property key to environment variable name. Trident does not
     * derive environment variable names by reflection or by transforming property keys: a key
     * is readable from the environment only if it is listed here.
     */
    private static final Map<String, String> PROPERTY_KEYS_TO_ENVIRONMENT_VARIABLES = Map.of(
            KEY_ENV,
            "ENV",
            "web.base.url",
            "WEB_BASE_URL",
            "api.base.url",
            "API_BASE_URL",
            "timeout.default.seconds",
            "TIMEOUT_DEFAULT_SECONDS",
            "timeout.polling.millis",
            "TIMEOUT_POLLING_MILLIS",
            "browser",
            "BROWSER",
            "browser.headless",
            "BROWSER_HEADLESS");

    private ConfigLoader() {}

    static TridentConfig load(String env, Map<String, String> systemProps, Map<String, String> envVars) {
        Map<String, String> fromSystemProperties = select(systemProps);

        // The highest-priority imported map must carry the resolved environment, whether or not
        // the caller supplied one, so that TridentConfig.env() reports the environment the
        // configuration was actually built for.
        fromSystemProperties.put(KEY_ENV, env);

        Map<String, String> fromEnvironmentVariables = mapFromEnvironment(envVars);

        // Imported maps outrank @Sources for value lookup, but they do NOT feed the expansion of
        // ${env} in the @Sources URIs: Owner expands those from its factory-level properties.
        // Passing env in the imported map alone therefore leaves ${env} unexpanded and
        // config/<env>.properties unread. A fresh Factory per call carries that property without
        // mutating the global ConfigFactory, which would be shared static state and would leak
        // one caller's environment into the next.
        Factory factory = ConfigFactory.newInstance();
        factory.setProperty(KEY_ENV, env);

        return factory.create(TridentConfig.class, fromSystemProperties, fromEnvironmentVariables);
    }

    /** Copies only the seven known keys across, ignoring everything else in the JVM. */
    private static Map<String, String> select(Map<String, String> systemProps) {
        Map<String, String> selected = new HashMap<>();
        for (String propertyKey : PROPERTY_KEYS_TO_ENVIRONMENT_VARIABLES.keySet()) {
            String value = systemProps.get(propertyKey);
            if (value != null) {
                selected.put(propertyKey, value);
            }
        }
        return selected;
    }

    /** Translates environment variable names onto property keys using the closed table. */
    private static Map<String, String> mapFromEnvironment(Map<String, String> envVars) {
        Map<String, String> mapped = new HashMap<>();
        for (Map.Entry<String, String> mapping : PROPERTY_KEYS_TO_ENVIRONMENT_VARIABLES.entrySet()) {
            String value = envVars.get(mapping.getValue());
            if (value != null) {
                mapped.put(mapping.getKey(), value);
            }
        }
        return mapped;
    }
}
