/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.MetricsRegistryListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.strimzi.kafka.metrics.yammer.YammerCollector;
import io.strimzi.kafka.metrics.yammer.YammerMetricWrapper;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.apache.kafka.server.metrics.KafkaYammerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * KafkaMetricsReporter to export Kafka broker metrics in the Prometheus format.
 */
public class BrokerYammerPrometheusMetricsReporter extends AbstractReporter implements KafkaMetricsReporter, MetricsRegistryListener {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerYammerPrometheusMetricsReporter.class);
    private static BrokerYammerPrometheusMetricsReporter INSTANCE;

    private final PrometheusRegistry registry;
    private final YammerCollector yammerCollector;
    @SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"}) // This field is initialized in the init method
    /* test */ ServerMetricsReporterConfig config;

    public static BrokerYammerPrometheusMetricsReporter getInstance() {
        return INSTANCE;
    }

    /**
     * Constructor
     */
    public BrokerYammerPrometheusMetricsReporter() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Cannot create multiple instances of BrokerYammerPrometheusMetricsReporter");
        }
        INSTANCE = this;
        registry = PrometheusRegistry.defaultRegistry;
        yammerCollector = YammerCollector.getCollector(PrometheusCollector.register(registry));
        yammerCollector.addReporter(this);
    }

    // for testing
    BrokerYammerPrometheusMetricsReporter(PrometheusRegistry registry, PrometheusCollector prometheusCollector) {
        this.registry = registry;
        yammerCollector = YammerCollector.getCollector(prometheusCollector);
        yammerCollector.addReporter(this);
    }

    @Override
    public void init(VerifiableProperties props) {
        config = new ServerMetricsReporterConfig(props.props(), registry);
        for (MetricsRegistry yammerRegistry : Arrays.asList(KafkaYammerMetrics.defaultRegistry(), Metrics.defaultRegistry())) {
            yammerRegistry.addListener(this);
        }
        LOG.debug("BrokerYammerPrometheusMetricsReporter configured with {}", config);
    }

    @Override
    public void onMetricAdded(MetricName name, Metric metric) {
        String prometheusName = YammerMetricWrapper.prometheusName(name);
        MetricWrapper metricWrapper = new YammerMetricWrapper(prometheusName, name.getScope(), metric, name.getName());
        addMetric(name, metricWrapper);
    }

    @Override
    public void onMetricRemoved(MetricName name) {
        removeMetric(name);
    }


    public void reconfigure(Map<String, ?> configs) {
        config.reconfigure(configs);
        updateAllowedMetrics();
    }

    @Override
    protected Pattern allowlist() {
        return config.allowlist();
    }

    @Override
    protected boolean isReconfigurable() {
        return true;
    }
}
