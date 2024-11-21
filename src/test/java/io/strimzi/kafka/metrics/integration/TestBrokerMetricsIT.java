/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics.integration;

import io.strimzi.kafka.metrics.KafkaPrometheusMetricsReporter;
import io.strimzi.kafka.metrics.MetricsUtils;
import io.strimzi.kafka.metrics.PrometheusMetricsReporterConfig;
import io.strimzi.kafka.metrics.YammerPrometheusMetricsReporter;
import io.strimzi.kafka.metrics.http.Listener;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBrokerMetricsIT {

    private static final String VERSION = "1.0.0-SNAPSHOT";
    private static final String REPORTER_JARS = "target/metrics-reporter-" + VERSION + "/metrics-reporter-" + VERSION + "/libs/";
    private static final String MOUNT_PATH = "/opt/strimzi/metrics-reporter/";
    private static final int PORT = Listener.parseListener(PrometheusMetricsReporterConfig.LISTENER_CONFIG_DEFAULT).port;

    private Map<String, String> configs;
    private StrimziKafkaContainer broker;

    @BeforeEach
    public void setUp() {
        configs = new HashMap<>();
        configs.put("metric.reporters", KafkaPrometheusMetricsReporter.class.getName());
        configs.put("kafka.metrics.reporters", YammerPrometheusMetricsReporter.class.getName());

        broker = new StrimziKafkaContainer()
                .waitForRunning()
                .withKraft()
                .withNodeId(0)
                .withCopyFileToContainer(MountableFile.forHostPath(REPORTER_JARS), MOUNT_PATH)
                .withExposedPorts(9092, PORT)
                .withKafkaConfigurationMap(configs)
                .withEnv(Collections.singletonMap("CLASSPATH", MOUNT_PATH + "*"));
    }

    @AfterEach
    public void tearDown() {
        broker.stop();
    }

    @Test
    public void testMetricsReporter() throws Exception {
        broker.start();
        List<String> metrics = MetricsUtils.getMetrics(broker.getHost(), broker.getMappedPort(PORT));
        List<String> prefixes = Arrays.asList(
                "jvm_",
                "kafka_controller_",
                "kafka_coordinator_",
                "kafka_log_",
                "kafka_network_",
                "kafka_server_");
        for (String prefix : prefixes) {
            assertFalse(filterMetrics(metrics, prefix).isEmpty());
        }
    }

    @Test
    public void testMetricsReporterWithAllowlist() throws Exception {
        configs.put("prometheus.metrics.reporter.allowlist", "kafka_controller.*,kafka_server.*");
        broker.withKafkaConfigurationMap(configs);
        broker.start();
        List<String> metrics = MetricsUtils.getMetrics(broker.getHost(), broker.getMappedPort(PORT));
        List<String> allowedPrefixes = Arrays.asList(
                "jvm_",
                "kafka_controller_",
                "kafka_server_");
        for (String prefix : allowedPrefixes) {
            assertFalse(filterMetrics(metrics, prefix).isEmpty());
        }
        List<String> disallowPrefixes = Arrays.asList(
                "kafka_coordinator_",
                "kafka_log_",
                "kafka_network_");
        for (String prefix : disallowPrefixes) {
            assertTrue(filterMetrics(metrics, prefix).isEmpty());
        }
    }

    @Test
    public void testReconfigureMetricsReporterWithAllowlist() throws Exception {
        configs.put("prometheus.metrics.reporter.allowlist", "kafka_controller.*,kafka_server.*");
        broker.withKafkaConfigurationMap(configs);
        broker.start();
        List<String> metrics = MetricsUtils.getMetrics(broker.getHost(), broker.getMappedPort(PORT));
        List<String> allowedPrefixes = Arrays.asList(
                "jvm_",
                "kafka_controller_",
                "kafka_server_");
        for (String prefix : allowedPrefixes) {
            assertFalse(filterMetrics(metrics, prefix).isEmpty());
        }
        List<String> disallowPrefixes = Arrays.asList(
                "kafka_coordinator_",
                "kafka_log_",
                "kafka_network_");
        for (String prefix : disallowPrefixes) {
            assertTrue(filterMetrics(metrics, prefix).isEmpty());
        }

        Map<String, Object> adminConfigs = new HashMap<>();
        System.out.println(broker.getBootstrapServers());
        adminConfigs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBootstrapServers());
        Admin admin = Admin.create(adminConfigs);
        Map<ConfigResource, Collection<AlterConfigOp>> configsToAlter = Collections.singletonMap(
                new ConfigResource(ConfigResource.Type.BROKER, ""),
                Collections.singleton(new AlterConfigOp(
                        new ConfigEntry(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_controller.*,kafka_server.*,kafka_log.*"),
                        AlterConfigOp.OpType.SET
                ))
        );
        admin.incrementalAlterConfigs(configsToAlter).all().get();

        Map<ConfigResource, Config> conf = admin.describeConfigs(Collections.singletonList(new ConfigResource(ConfigResource.Type.BROKER, ""))).all().get();
        System.out.println(conf);
        //Thread.sleep(100000000L);
        metrics = MetricsUtils.getMetrics(broker.getHost(), broker.getMappedPort(PORT));
        System.out.println(metrics);
        allowedPrefixes = Arrays.asList(
                "jvm_",
                "kafka_controller_",
                "kafka_server_",
                "kafka_log_");
        for (String prefix : allowedPrefixes) {
            System.out.println("checking prefix " + prefix);
            assertFalse(filterMetrics(metrics, prefix).isEmpty());
        }
        disallowPrefixes = Arrays.asList(
                "kafka_coordinator_",
                "kafka_network_");
        for (String prefix : disallowPrefixes) {
            assertTrue(filterMetrics(metrics, prefix).isEmpty());
        }
    }

    private List<String> filterMetrics(List<String> allMetrics, String prefix) {
        List<String> metrics = new ArrayList<>();
        for (String metric : allMetrics) {
            if (metric.startsWith(prefix)) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

}
