/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.apache.kafka.common.config.AbstractConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
* Configuration for the PrometheusMetricsReporter implementation.
*/
public class ServerMetricsReporterConfig extends ClientMetricsReporterConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ServerMetricsReporterConfig.class);

    public static final Set<String> RECONFIGURABLES = Set.of(ALLOWLIST_CONFIG);

    private Pattern allowlist;

    /**
     * Constructor.
     *
     * @param props the configuration properties.
     * @param registry the metrics registry
     */
    public ServerMetricsReporterConfig(Map<?, ?> props, PrometheusRegistry registry) {
        super(props, registry);
        this.allowlist = compileAllowlist(getList(ALLOWLIST_CONFIG));
    }

    public void reconfigure(Map<String, ?> props) {
        AbstractConfig abstractConfig = new AbstractConfig(CONFIG_DEF, props);
        allowlist = compileAllowlist(abstractConfig.getList(ALLOWLIST_CONFIG));
        LOG.info("Updated allowlist to {}", allowlist);
    }

    @Override
    public Pattern allowlist() {
        return allowlist;
    }

    @Override
    public String toString() {
        return "ServerMetricsReporterConfig{" +
                ", listener=" + listener +
                ", listenerEnabled=" + listenerEnabled +
                ", allowlist=" + allowlist +
                '}';
    }
}
