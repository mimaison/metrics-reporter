/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import io.strimzi.kafka.metrics.kafka.KafkaCollector;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.metrics.MetricsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * MetricsReporter implementation that expose Kafka broker metrics in the Prometheus format.
 */
public class KafkaBrokerPrometheusMetricsReporter extends KafkaClientPrometheusMetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaClientPrometheusMetricsReporter.class);
    private final static String PREFIX = "kafka.server";

    // for testing
    KafkaBrokerPrometheusMetricsReporter(PrometheusRegistry registry, KafkaCollector kafkaCollector) {
        super(registry, kafkaCollector);
    }

    @Override
    public void reconfigure(Map<String, ?> configs) {
        config.reconfigure(configs);
        YammerPrometheusMetricsReporter yammerReporter = YammerPrometheusMetricsReporter.getInstance();
        if (yammerReporter != null) {
            yammerReporter.reconfigure(configs);
        }
    }

    @Override
    public void validateReconfiguration(Map<String, ?> configs) throws ConfigException {
        new PrometheusMetricsReporterConfig(configs, null);
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return PrometheusMetricsReporterConfig.RECONFIGURABLES;
    }

    @Override
    public void contextChange(MetricsContext metricsContext) {
        String prefix = metricsContext.contextLabels().get(MetricsContext.NAMESPACE);
        if (!PREFIX.equals(prefix)) {
            throw new IllegalStateException("KafkaBrokerPrometheusMetricsReporter should only be used in Kafka servers");
        }
        this.prefix = PrometheusNaming.prometheusName(prefix);
    }
}
