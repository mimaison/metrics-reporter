/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics.prometheus.http;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListenerTest {

    @Test
    public void testListenerParseListener() {
        assertEquals(new Listener("", 8080), Listener.parseListener("http://:8080"));
        assertEquals(new Listener("123", 8080), Listener.parseListener("http://123:8080"));
        assertEquals(new Listener("::1", 8080), Listener.parseListener("http://::1:8080"));
        assertEquals(new Listener("::1", 8080), Listener.parseListener("http://[::1]:8080"));
        assertEquals(new Listener("random", 8080), Listener.parseListener("http://random:8080"));

        assertThrows(ConfigException.class, () -> Listener.parseListener("http"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://random"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://random:"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://:-8080"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://random:-8080"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("http://:8080random"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("randomhttp://:8080random"));
        assertThrows(ConfigException.class, () -> Listener.parseListener("randomhttp://:8080"));
    }

    @Test
    public void testValidator() {
        Listener.ListenerValidator validator = new Listener.ListenerValidator();
        validator.ensureValid("name", "http://:0");
        validator.ensureValid("name", "http://123:8080");
        validator.ensureValid("name", "http://::1:8080");
        validator.ensureValid("name", "http://[::1]:8080");
        validator.ensureValid("name", "http://random:8080");

        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://random:"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://:-8080"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://random:-8080"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "http://:8080random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "randomhttp://:8080random"));
        assertThrows(ConfigException.class, () -> validator.ensureValid("name", "randomhttp://:8080"));
    }
}
