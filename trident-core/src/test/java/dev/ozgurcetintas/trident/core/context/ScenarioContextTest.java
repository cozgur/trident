package dev.ozgurcetintas.trident.core.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScenarioContextTest {

    private final ScenarioContext context = new ScenarioContext();

    @Test
    @DisplayName("A stored value is returned by get with its type")
    void roundTripsAValue() {
        context.put("orderId", "A-1001");
        context.put("itemCount", 3);

        assertThat(context.get("orderId", String.class)).isEqualTo("A-1001");
        assertThat(context.get("itemCount", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("An absent key throws TridentException naming the key")
    void rejectsAbsentKey() {
        assertThatThrownBy(() -> context.get("orderId", String.class))
                .isInstanceOf(TridentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    @DisplayName("A type mismatch throws TridentException naming both types")
    void rejectsTypeMismatch() {
        context.put("itemCount", 3);

        assertThatThrownBy(() -> context.get("itemCount", String.class))
                .isInstanceOf(TridentException.class)
                .hasMessageContaining("itemCount")
                .hasMessageContaining(Integer.class.getName())
                .hasMessageContaining(String.class.getName());
    }

    @Test
    @DisplayName("contains reports true before clear and false after")
    void reportsContainmentAcrossClear() {
        context.put("orderId", "A-1001");
        assertThat(context.contains("orderId")).isTrue();

        context.clear();
        assertThat(context.contains("orderId")).isFalse();
    }

    @Test
    @DisplayName("A null key and a null value are each rejected")
    void rejectsNulls() {
        assertThatThrownBy(() -> context.put(null, "A-1001"))
                .isInstanceOf(TridentException.class)
                .hasMessageContaining("null key");

        assertThatThrownBy(() -> context.put("orderId", null))
                .isInstanceOf(TridentException.class)
                .hasMessageContaining("orderId");

        assertThat(context.contains("orderId")).isFalse();
    }
}
