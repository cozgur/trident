package dev.ozgurcetintas.trident.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises stage 2 of the bootstrap directly. Every test builds its own configuration, so
 * none of them touches the instance cached by {@link ConfigProvider}.
 */
class ConfigLoaderTest {

    @Test
    @DisplayName("MERGE falls back to default.properties for keys local.properties omits")
    void mergesEnvironmentFileOverDefaults() {
        TridentConfig config = ConfigLoader.load("local", Map.of(), Map.of());

        // local.properties overrides only this key.
        assertThat(config.defaultTimeoutSeconds()).isEqualTo(5L);

        // The remaining keys are absent from local.properties and must fall back.
        assertThat(config.webBaseUrl()).isEqualTo("http://localhost");
        assertThat(config.apiBaseUrl()).isEqualTo("http://localhost");
        assertThat(config.pollingMillis()).isEqualTo(200L);
    }

    @Test
    @DisplayName("A system property beats a file value")
    void systemPropertyBeatsFile() {
        TridentConfig config =
                ConfigLoader.load("local", Map.of("web.base.url", "http://from-system-property"), Map.of());

        assertThat(config.webBaseUrl()).isEqualTo("http://from-system-property");

        // Keys without an override still come from the files.
        assertThat(config.defaultTimeoutSeconds()).isEqualTo(5L);
        assertThat(config.apiBaseUrl()).isEqualTo("http://localhost");
    }

    @Test
    @DisplayName("An environment variable beats a file value but loses to a system property")
    void environmentVariableBeatsFileAndLosesToSystemProperty() {
        TridentConfig fromEnvironment = ConfigLoader.load("local", Map.of(), Map.of("WEB_BASE_URL", "http://from-env"));

        assertThat(fromEnvironment.webBaseUrl()).isEqualTo("http://from-env");

        TridentConfig bothSupplied = ConfigLoader.load(
                "local",
                Map.of("web.base.url", "http://from-system-property"),
                Map.of("WEB_BASE_URL", "http://from-env"));

        assertThat(bothSupplied.webBaseUrl()).isEqualTo("http://from-system-property");
    }
}
