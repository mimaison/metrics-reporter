/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics.prometheus.kafka;

import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.strimzi.kafka.metrics.prometheus.common.AbstractReporter;
import io.strimzi.kafka.metrics.prometheus.common.MetricWrapper;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.Gauge;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static io.strimzi.kafka.metrics.prometheus.KafkaMetricsUtils.newKafkaMetric;
import static io.strimzi.kafka.metrics.prometheus.MetricsUtils.assertGaugeSnapshot;
import static io.strimzi.kafka.metrics.prometheus.MetricsUtils.assertInfoSnapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KafkaCollectorTest {

    private static final String METRIC_PREFIX = "kafka.server";

    private Map<String, String> tagsMap;
    private Labels labels;

    @BeforeEach
    public void setup() {
        tagsMap = new LinkedHashMap<>();
        Labels.Builder labelsBuilder = Labels.builder();
        for (int i = 0; i < 2; i++) {
            labelsBuilder.label("k" + i, "v" + i);
            tagsMap.put("k" + i, "v" + i);
        }
        labels = labelsBuilder.build();
    }

    @Test
    public void testCollectKafkaMetrics() {
        KafkaCollector collector = new KafkaCollector();
        AbstractReporter reporter = new AbstractReporter() {
            @Override
            protected Pattern allowlist() {
                return Pattern.compile(".*");
            }
        };
        collector.addReporter(reporter);

        List<? extends MetricSnapshot> metrics = collector.collect();
        assertEquals(0, metrics.size());

        // Adding a metric
        AtomicInteger value = new AtomicInteger(1);
        MetricName metricName = new MetricName("name", "group", "description", tagsMap);
        MetricWrapper metricWrapper = newKafkaMetricWrapper(metricName, (config, now) -> value.get());
        reporter.addMetric(metricName, metricWrapper);

        metrics = collector.collect();
        assertEquals(1, metrics.size());
        assertGaugeSnapshot(metrics.get(0), value.get(), labels);

        // Updating the value of the metric
        value.set(3);
        metrics = collector.collect();
        assertEquals(1, metrics.size());
        assertGaugeSnapshot(metrics.get(0), 3, labels);

        // Removing a metric
        reporter.removeMetric(metricName);
        metrics = collector.collect();
        assertEquals(0, metrics.size());

        collector.removeReporter(reporter);
        metrics = collector.collect();
        assertEquals(0, metrics.size());
    }

    @Test
    public void testCollectNonNumericKafkaMetric() {
        KafkaCollector collector = new KafkaCollector();
        AbstractReporter reporter = new AbstractReporter() {
            @Override
            protected Pattern allowlist() {
                return Pattern.compile(".*");
            }
        };
        collector.addReporter(reporter);

        List<? extends MetricSnapshot> metrics = collector.collect();
        assertEquals(0, metrics.size());

        // Adding a non-numeric metric converted
        String nonNumericValue = "myValue";
        MetricName metricName = new MetricName("name", "group", "description", tagsMap);
        MetricWrapper metricWrapper = newKafkaMetricWrapper(metricName, (config, now) -> nonNumericValue);
        reporter.addMetric(metricName, metricWrapper);

        metrics = collector.collect();
        assertEquals(1, metrics.size());
        MetricSnapshot snapshot = metrics.get(0);
        assertEquals(metricWrapper.prometheusName(), snapshot.getMetadata().getName());
        assertInfoSnapshot(snapshot, labels, "name", nonNumericValue);

        collector.removeReporter(reporter);
        metrics = collector.collect();
        assertEquals(0, metrics.size());
    }

    private MetricWrapper newKafkaMetricWrapper(MetricName metricName, Gauge<?> gauge) {
        KafkaMetric kafkaMetric = newKafkaMetric(metricName.name(), metricName.group(), gauge, metricName.tags());
        String prometheusName = KafkaMetricWrapper.prometheusName(METRIC_PREFIX, metricName);
        return new KafkaMetricWrapper(prometheusName, kafkaMetric, metricName.name());
    }

}
