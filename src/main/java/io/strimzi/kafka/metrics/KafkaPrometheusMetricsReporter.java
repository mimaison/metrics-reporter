/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import io.strimzi.kafka.metrics.http.HttpServers;
import io.strimzi.kafka.metrics.kafka.KafkaCollector;
import io.strimzi.kafka.metrics.kafka.KafkaMetricWrapper;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsContext;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * MetricsReporter implementation that expose Kafka metrics in the Prometheus format.
 * This can be used by Kafka brokers and clients.
 */
public class KafkaPrometheusMetricsReporter extends AbstractReporter implements MetricsReporter  {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaPrometheusMetricsReporter.class);

    private static final String BROKER_PREFIX = "kafka_server";

    private final PrometheusRegistry registry;
    private final KafkaCollector kafkaCollector;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the configure method
    private PrometheusMetricsReporterConfig config;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the configure method
    private Optional<HttpServers.ServerCounter> httpServer;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the contextChange method
    private String prefix;
    private Map<String, ?> configs;

    /**
     * Constructor
     */
    public KafkaPrometheusMetricsReporter() {
        registry = PrometheusRegistry.defaultRegistry;
        kafkaCollector = KafkaCollector.getCollector(PrometheusCollector.register(registry));
        kafkaCollector.addReporter(this);
    }

    // for testing
    KafkaPrometheusMetricsReporter(PrometheusRegistry registry, KafkaCollector kafkaCollector) {
        this.registry = registry;
        this.kafkaCollector = kafkaCollector;
        this.kafkaCollector.addReporter(this);
    }

    @Override
    public void configure(Map<String, ?> map) {
        configs = map;
    }

    @Override
    public void init(List<KafkaMetric> metrics) {
        for (KafkaMetric metric : metrics) {
            metricChange(metric);
        }
    }

    @Override
    public void metricChange(KafkaMetric metric) {
        String prometheusName = KafkaMetricWrapper.prometheusName(prefix, metric.metricName());
        MetricWrapper metricWrapper = new KafkaMetricWrapper(prometheusName, metric, metric.metricName().name());
        addMetric(metric, metricWrapper);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        removeMetric(metric);
    }

    @Override
    public void close() {
        kafkaCollector.removeReporter(this);
        httpServer.ifPresent(HttpServers::release);
    }

    @Override
    public void reconfigure(Map<String, ?> configs) {
        config.updateAllowlist(configs);
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
        this.prefix = PrometheusNaming.prometheusName(prefix);
        if (BROKER_PREFIX.equals(this.prefix)) {
            config = PrometheusMetricsReporterConfig.getBrokerInstance(configs, registry);
            config.addListener(this);
        } else {
            config = new PrometheusMetricsReporterConfig(configs, registry);
        }
        httpServer = config.startHttpServer();
        LOG.debug("KafkaPrometheusMetricsReporter configured with {}", config);
    }

    @Override
    protected PrometheusMetricsReporterConfig config() {
        return config;
    }

    // for testing
    Optional<Integer> getPort() {
        return Optional.ofNullable(httpServer.isPresent() ? httpServer.get().port() : null);
    }
}
