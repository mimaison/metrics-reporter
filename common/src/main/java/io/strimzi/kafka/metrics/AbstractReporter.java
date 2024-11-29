/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public abstract class AbstractReporter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractReporter.class);

    private final Map<Object, MetricWrapper> allowedMetrics = new ConcurrentHashMap<>();
    private final Map<Object, MetricWrapper> otherMetrics = new ConcurrentHashMap<>();

    protected abstract Pattern allowlist();
    protected abstract boolean isReconfigurable();

    private boolean matches(String name) {
        return allowlist().matcher(name).matches();
    }

    /**
     * Add a metric to be collected.
     * @param name The name of the metric to add.
     * @param metric The metric to add.
     */
    void addMetric(Object name, MetricWrapper metric) {
        if (matches(metric.prometheusName())) {
            allowedMetrics.put(name, metric);
        } else {
            LOG.trace("Ignoring metric {} as it does not match the allowlist", metric.prometheusName());
            if (isReconfigurable()) {
                otherMetrics.put(name, metric);
            }
        }
    }

    /**
     * Remove a metric from collection.
     * @param name The name of metric to remove.
     */
    void removeMetric(Object name) {
        allowedMetrics.remove(name);
        if (isReconfigurable()) {
            otherMetrics.remove(name);
        }
    }

    /**
     * Retrieve the allowed metrics
     * @return the collection of allowed MetricWrapper
     */
    public Collection<MetricWrapper> allowedMetrics() {
        return allowedMetrics.values();
    }

    public void updateAllowedMetrics() {
        if (!isReconfigurable()) return;
        Map<Object, MetricWrapper> newAllowedMetrics = new HashMap<>();
        for (Map.Entry<Object, MetricWrapper> entry : otherMetrics.entrySet()) {
            String name = entry.getValue().prometheusName();
            if (matches(name)) {
                newAllowedMetrics.put(entry.getKey(), entry.getValue());
                otherMetrics.remove(entry.getKey());
            }
        }
        for (Map.Entry<Object, MetricWrapper> entry : allowedMetrics.entrySet()) {
            String name = entry.getValue().prometheusName();
            if (!matches(name)) {
                otherMetrics.put(entry.getKey(), entry.getValue());
                allowedMetrics.remove(entry.getKey());
            }
        }
        allowedMetrics.putAll(newAllowedMetrics);
    }
}