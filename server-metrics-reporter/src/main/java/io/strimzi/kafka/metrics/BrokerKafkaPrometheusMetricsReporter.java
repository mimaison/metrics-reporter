/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class BrokerKafkaPrometheusMetricsReporter extends ClientPrometheusMetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerKafkaPrometheusMetricsReporter.class);
    private final static String PREFIX = "kafka.server";

    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the configure method
    ServerMetricsReporterConfig config;

    // for testing
    BrokerKafkaPrometheusMetricsReporter(PrometheusRegistry registry, KafkaCollector kafkaCollector) {
        super(registry, kafkaCollector);
    }

    @Override
    public void configure(Map<String, ?> map) {
        config = new ServerMetricsReporterConfig(map, registry);
        httpServer = config.startHttpServer();
        LOG.debug("ClientPrometheusMetricsReporter configured with {}", config);
    }

    @Override
    public void reconfigure(Map<String, ?> configs) {
        config.reconfigure(configs);
        BrokerYammerPrometheusMetricsReporter yammerReporter = BrokerYammerPrometheusMetricsReporter.getInstance();
        if (yammerReporter != null) {
            yammerReporter.reconfigure(configs);
        }
        updateAllowedMetrics();
    }

    @Override
    public void validateReconfiguration(Map<String, ?> configs) throws ConfigException {
        new ServerMetricsReporterConfig(configs, null);
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return ServerMetricsReporterConfig.RECONFIGURABLES;
    }

    @Override
    public void contextChange(MetricsContext metricsContext) {
        String prefix = metricsContext.contextLabels().get(MetricsContext.NAMESPACE);
        if (!PREFIX.equals(prefix)) {
            throw new IllegalStateException("BrokerKafkaPrometheusMetricsReporter should only be used in Kafka servers");
        }
        this.prefix = PrometheusNaming.prometheusName(prefix);
    }

    @Override
    protected boolean isReconfigurable() {
        return true;
    }
}
