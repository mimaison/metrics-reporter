/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.strimzi.kafka.metrics.ClientMetricsReporterConfig.*;
import static org.junit.jupiter.api.Assertions.*;


public class ServerMetricsReporterConfigTest {

    @Test
    public void testReconfigure() {
        Map<String, String> props = new HashMap<>();
        props.put(ALLOWLIST_CONFIG, "pattern1");
        ServerMetricsReporterConfig config = new ServerMetricsReporterConfig(props, new PrometheusRegistry());
        assertTrue(config.allowlist().pattern().contains("pattern1"));

        props.put(ALLOWLIST_CONFIG, "pattern2");
        config.reconfigure(props);
        System.out.println(config.allowlist());
        assertFalse(config.allowlist().pattern().contains("pattern1"));
        assertTrue(config.allowlist().pattern().contains("pattern2"));
    }
}

