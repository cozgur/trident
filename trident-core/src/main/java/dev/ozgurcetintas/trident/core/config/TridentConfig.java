package dev.ozgurcetintas.trident.core.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

/**
 * Typed view over Trident's configuration.
 *
 * <p>The load policy is {@link LoadType#MERGE} rather than Owner's default {@code FIRST}.
 * {@code FIRST} stops at the first source that exists, which would make
 * {@code default.properties} unreachable whenever an environment-specific file is present.
 * {@code MERGE} makes the two sources behave as a fallback chain: a key defined in
 * {@code config/${env}.properties} wins, and every key it omits falls back to
 * {@code config/default.properties}.
 *
 * <p><strong>Not an extension point.</strong> Owner generates the implementation; nothing is
 * expected to implement this interface by hand. That is what lets a key be added in a minor
 * release — {@code browser} and {@code browser.headless} arrived in 1.1.0 — without it being a
 * breaking change, because adding an abstract method breaks implementors and there are none.
 * Add a value of your own in a properties file, or override one with a system property or the
 * mapped environment variable; do not subclass this to get there.
 */
@LoadPolicy(LoadType.MERGE)
@Sources({"classpath:config/${env}.properties", "classpath:config/default.properties"})
public interface TridentConfig extends Config {

    String env();

    @Key("web.base.url")
    String webBaseUrl();

    @Key("api.base.url")
    String apiBaseUrl();

    @Key("timeout.default.seconds")
    long defaultTimeoutSeconds();

    @Key("timeout.polling.millis")
    long pollingMillis();

    /**
     * Which browser the web layer drives. Selenium Manager resolves the binary and the driver,
     * so this is the only thing a consumer has to say.
     */
    @Key("browser")
    String browser();

    /**
     * Whether that browser runs without a visible window. Defaults to {@code true} because CI
     * is the common case and a headed run is the exception a developer asks for by name.
     */
    @Key("browser.headless")
    boolean headless();
}
