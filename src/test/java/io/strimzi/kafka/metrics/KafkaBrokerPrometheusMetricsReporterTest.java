/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.KafkaMetricsContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static io.strimzi.kafka.metrics.MetricsUtils.getMetrics;
import static io.strimzi.kafka.metrics.MetricsUtils.newKafkaMetric;
import static org.junit.jupiter.api.Assertions.*;

public class KafkaBrokerPrometheusMetricsReporterTest extends KafkaClientPrometheusMetricsReporterTest {

    @Test
    public void testReconfigurableConfigs() {
        KafkaBrokerPrometheusMetricsReporter reporter = new KafkaBrokerPrometheusMetricsReporter(registry, kafkaCollector);
        assertFalse(reporter.reconfigurableConfigs().isEmpty());
    }

    @Test
    public void testReconfigure() throws Exception {
        KafkaBrokerPrometheusMetricsReporter reporter = new KafkaBrokerPrometheusMetricsReporter(registry, kafkaCollector);
        configs.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server_group_name.*");
        reporter.configure(configs);
        reporter.contextChange(new KafkaMetricsContext("kafka.server"));

        int port = reporter.getPort().orElseThrow();
        assertEquals(0, getMetrics(port).size());

        // Adding a metric not matching the allowlist does nothing
        KafkaMetric metric1 = newKafkaMetric("other", "group", (config, now) -> 0, labels);
        reporter.init(Collections.singletonList(metric1));
        List<String> metrics = getMetrics(port);
        assertEquals(0, metrics.size());

        // Adding a metric matching the allowlist
        KafkaMetric metric2 = newKafkaMetric("name", "group", (config, now) -> 0, labels);
        reporter.metricChange(metric2);
        metrics = getMetrics(port);
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).contains("name"));

        configs.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server_group_other.*");
        reporter.reconfigure(configs);

        metrics = getMetrics(port);
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).contains("other"));
    }

}
