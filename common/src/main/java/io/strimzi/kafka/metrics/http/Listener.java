/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics.http;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class parsing and handling the listener for the HTTP server used to expose the metrics.
 */
public class Listener {

    private static final Pattern PATTERN = Pattern.compile("http://\\[?([0-9a-zA-Z\\-%._:]*)]?:([0-9]+)");

    /**
     * The host of the listener
     */
    public final String host;
    /**
     * The port of the listener
     */
    public final int port;

    /* test */ Listener(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Build a Listener instance from a "http://[host]:[port]" string
     * @param listener the input string
     * @return the listener
     */
    public static Listener parseListener(String configName, String listener) {
        Matcher matcher = PATTERN.matcher(listener);
        if (matcher.matches()) {
            String host = matcher.group(1);
            int port = Integer.parseInt(matcher.group(2));
            return new Listener(host, port);
        } else {
            throw new ConfigException(configName, listener, "Listener must be of format http://[host]:[port]");
        }
    }

    @Override
    public String toString() {
        return "http://" + host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Listener listener = (Listener) o;
        return port == listener.port && Objects.equals(host, listener.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    /**
     * Validator to check the user provided listener configuration
     */
    public static class ListenerValidator implements ConfigDef.Validator {

        @Override
        public void ensureValid(String name, Object value) {
            Matcher matcher = PATTERN.matcher(String.valueOf(value));
            if (!matcher.matches()) {
                throw new ConfigException(name, value, "Listener must be of format http://[host]:[port]");
            }
        }
    }
}
