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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MetricsReporter implementation that expose Kafka client metrics in the Prometheus format.
 */
public class ClientPrometheusMetricsReporter extends AbstractReporter implements MetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(ClientPrometheusMetricsReporter.class);

    final PrometheusRegistry registry;
    private final KafkaCollector kafkaCollector;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the configure method
    ClientMetricsReporterConfig config;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the configure method
    Optional<HttpServers.ServerCounter> httpServer;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the contextChange method
    String prefix;

    /**
     * Constructor
     */
    public ClientPrometheusMetricsReporter() {
        registry = PrometheusRegistry.defaultRegistry;
        kafkaCollector = KafkaCollector.getCollector(PrometheusCollector.register(registry));
        kafkaCollector.addReporter(this);
    }

    // for testing
    ClientPrometheusMetricsReporter(PrometheusRegistry registry, KafkaCollector kafkaCollector) {
        this.registry = registry;
        this.kafkaCollector = kafkaCollector;
        kafkaCollector.addReporter(this);
    }

    @Override
    public void configure(Map<String, ?> map) {
        config = new ClientMetricsReporterConfig(map, registry);
        httpServer = config.startHttpServer();
        LOG.debug("ClientPrometheusMetricsReporter configured with {}", config);
    }

    @Override
    public void init(List<KafkaMetric> metrics) {
        for (KafkaMetric metric : metrics) {
            metricChange(metric);
        }
    }

    public void metricChange(KafkaMetric metric) {
        String prometheusName = KafkaMetricWrapper.prometheusName(prefix, metric.metricName());
        MetricWrapper metricWrapper = new KafkaMetricWrapper(prometheusName, metric, metric.metricName().name());
        addMetric(metric, metricWrapper);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        removeMetric(metric.metricName());
    }

    @Override
    public void close() {
        httpServer.ifPresent(HttpServers::release);
    }

    @Override
    public void reconfigure(Map<String, ?> configs) {
    }

    @Override
    public void validateReconfiguration(Map<String, ?> configs) throws ConfigException {
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return Collections.emptySet();
    }

    @Override
    public void contextChange(MetricsContext metricsContext) {
        String prefix = metricsContext.contextLabels().get(MetricsContext.NAMESPACE);
        this.prefix = PrometheusNaming.prometheusName(prefix);
    }

    // for testing
    Optional<Integer> getPort() {
        return Optional.ofNullable(httpServer.isPresent() ? httpServer.get().port() : null);
    }

    @Override
    protected Pattern allowlist() {
        return config.allowlist();
    }

    @Override
    protected boolean isReconfigurable() {
        return false;
    }
}
