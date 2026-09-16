package dev.ozgurcetintas.trident.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * A typed key/value store scoped to a single scenario.
 *
 * <p>Steps use it to hand values to later steps in the same scenario. The scope is the
 * scenario and nothing wider: the instance is created per scenario by Picocontainer and
 * injected into each step definition class that asks for one, so there is no static state and
 * no {@code ThreadLocal} holding a scenario's data.
 *
 * <p>{@link #get} is typed and total: it returns a value of the requested type or throws.
 * It never returns {@code null} and never returns an {@code Optional}, so a step reads a value
 * without a null check and a mistake surfaces as a named failure rather than a
 * {@code NullPointerException} several lines later.
 */
public class ScenarioContext {

    private final Map<String, Object> values = new HashMap<>();

    /**
     * Stores a value under a key, replacing any previous value for that key.
     *
     * @param key the key, not {@code null}
     * @param value the value, not {@code null}
     * @throws TridentException if the key or the value is {@code null}
     */
    public void put(String key, Object value) {
        if (key == null) {
            throw new TridentException("Cannot store a value in the scenario context under a null key");
        }
        if (value == null) {
            // A stored null is indistinguishable from an absent key at get() time, so permitting
            // it would reintroduce the null return this class exists to prevent.
            throw new TridentException("Cannot store a null value in the scenario context under key '" + key + "'");
        }
        values.put(key, value);
    }

    /**
     * Returns the value stored under a key, as the requested type.
     *
     * @param key the key to read
     * @param type the type the value is expected to have
     * @param <T> the expected type
     * @return the stored value, never {@code null}
     * @throws TridentException if the key is absent or the stored value is not of the expected
     *     type
     */
    public <T> T get(String key, Class<T> type) {
        if (!values.containsKey(key)) {
            throw new TridentException("No value in the scenario context under key '" + key + "'");
        }
        Object value = values.get(key);
        if (!type.isInstance(value)) {
            throw new TridentException("Value in the scenario context under key '" + key + "' is of type "
                    + value.getClass().getName() + ", not the expected " + type.getName());
        }
        return type.cast(value);
    }

    /**
     * Reports whether a value is stored under a key.
     *
     * @param key the key to check
     * @return {@code true} if a value is stored under that key
     */
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    /** Removes every value, leaving the context as it was when created. */
    public void clear() {
        values.clear();
    }
}
