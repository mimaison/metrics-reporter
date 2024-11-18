/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.metrics.integration;

import io.strimzi.kafka.metrics.MetricsUtils;
import io.strimzi.kafka.metrics.PrometheusMetricsReporterConfig;
import io.strimzi.kafka.metrics.http.Listener;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestConsumerMetricsIT {

    private static final String IMAGE = "quay.io/strimzi-test-clients/test-client-kafka-consumer:latest-kafka-3.4.0";
    private static final String VERSION = "1.0.0-SNAPSHOT";
    private static final String REPORTER_JARS = "target/metrics-reporter-" + VERSION + "/metrics-reporter-" + VERSION + "/libs/";
    private static final String MOUNT_PATH = "/opt/strimzi/metrics-reporter/";
    private static final int PORT = Listener.parseListener(PrometheusMetricsReporterConfig.LISTENER_CONFIG_DEFAULT).port;

    private StrimziKafkaContainer broker;

    @BeforeEach
    public void setUp() {
        broker = new StrimziKafkaContainer()
                .withNodeId(0)
                .withKraft()
                .withExposedPorts(9092)
                .withNetwork(Network.SHARED);
        broker.start();
    }

    @AfterEach
    public void tearDown() {
        broker.stop();
    }

    @Test
    public void testMetricsReporter() throws Exception {
        Map<String, String> configs = new HashMap<>();
        configs.put("BOOTSTRAP_SERVERS", broker.getBootstrapServers());
        configs.put("TOPIC", "mytopic");
        configs.put("ADDITIONAL_CONFIG_ENV", "metric.reporters=io.strimzi.kafka.metrics.KafkaPrometheusMetricsReporter");
        configs.put("LOG_LEVEL", "DEBUG");
        System.out.println(configs);

        GenericContainer<?> consumer = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withNetwork(Network.SHARED)
                .withCopyFileToContainer(MountableFile.forHostPath(REPORTER_JARS), MOUNT_PATH)
                .withExposedPorts(PORT)
                .withEnv(configs);

        consumer.start();

        List<String> metrics = MetricsUtils.getMetrics(consumer.getHost(), consumer.getMappedPort(PORT));
        assertFalse(metrics.isEmpty());
    }
}
