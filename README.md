[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Twitter Follow](https://img.shields.io/twitter/follow/strimziio?style=social)](https://twitter.com/strimziio)

# Strimzi Prometheus Metrics Reporter

Apache Kafka® brokers and clients expose metrics to monitor them. A pluggable reporter interface allows exporting these metrics to monitoring systems. Apache Kafka has a built-in reporter for JMX.

This repository contains a reporter implementation for Prometheus as proposed in [Strimzi Proposal #64](https://github.com/strimzi/proposals/blob/main/064-prometheus-metrics-reporter.md).

> [!WARNING]  
> The project is currently in early access.

## Installing

Download and extract the .tar.gz or .zip archive from the [latest release](https://github.com/strimzi/metrics-reporter/releases/latest) and
add all the JARs to the classpath.

## Building from source

Alternatively you can build the metrics reporter:
```sh
mvn package
```

After building, make sure the metrics reporter JARs located under `target/metrics-reporter-*/metrics-reporter-*/libs/` are in the classpath.

## Configuring

The metrics reporter has the following configurations:

- `prometheus.metrics.reporter.listener`: The HTTP listener to expose the metrics. It must be in the `http://[host]:[port]` format. This defaults to `http://:8080`.
- `prometheus.metrics.reporter.listener.enable`: Enable the listener to expose the metrics. This defaults to `true`.
- `prometheus.metrics.reporter.allowlist`: A comma separated list of regex patterns to specify the metrics to collect. This defaults to `.*`.

## Running

### Kafka Brokers and Controllers

To use the reporter with Kafka brokers and controllers, add the following to your broker configuration:

```properties
metric.reporters=io.strimzi.kafka.metrics.prometheus.ServerKafkaMetricsReporter
kafka.metrics.reporters=io.strimzi.kafka.metrics.prometheus.ServerYammerMetricsReporter
auto.include.jmx.reporter=false
```

The `prometheus.metrics.reporter.allowlist` configuration of brokers and controllers can be updated at runtime without restarting Kafka.

You can update the configuration using either of these approaches:

* The `kafka-configs.sh` command-line tool 
* The `incrementalAlterConfigs()` method from the `Admin` API. 

**Example update using `kafka-configs.sh`**
```sh
./bin/kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter --entity-type brokers --entity-default \
  --add-config "prometheus.metrics.reporter.allowlist=[kafka_controller.*,kafka_log.*]"
```

**Example update using `Admin` API**
```java
try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"))) {
    admin.incrementalAlterConfigs(Map.of(
        new ConfigResource(ConfigResource.Type.BROKER, ""),
        List.of(new AlterConfigOp(
            new ConfigEntry(ALLOWLIST_CONFIG, "kafka_controller.*,kafka_log.*"),
            AlterConfigOp.OpType.SET))
    )).all().get();
}
```

### Kafka Clients

To use the reporter with Kafka producers, consumers or admin clients, add the following to your client configuration:

```properties
metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
auto.include.jmx.reporter=false
```

### Kafka Connect and Kafka Streams

To use the reporter with Kafka Connect and Kafka Streams, add the following to your Connect runtime or Streams application configuration:

```properties
metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
auto.include.jmx.reporter=false
admin.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
admin.auto.include.jmx.reporter=false
producer.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
producer.auto.include.jmx.reporter=false
consumer.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
consumer.auto.include.jmx.reporter=false
```

When setting configurations for the Prometheus metrics reporter, they also need to be set with the `admin.`, `producer.` and `consumer.`.
For example, to set the `listener` to `http://:8081`:

```properties
metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
prometheus.metrics.reporter.listener=http://:8081
auto.include.jmx.reporter=false
admin.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
admin.prometheus.metrics.reporter.listener=http://:8081
admin.auto.include.jmx.reporter=false
producer.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
producer.prometheus.metrics.reporter.listener=http://:8081
producer.auto.include.jmx.reporter=false
consumer.metric.reporters=io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter
consumer.prometheus.metrics.reporter.listener=http://:8081
consumer.auto.include.jmx.reporter=false
```

### MirrorMaker

The Source and Checkpoint MirrorMaker connectors expose [metrics](https://kafka.apache.org/documentation/#georeplication-monitoring). To collect these metrics with the reporter, add the metrics-reporter configurations to the connector configurations. Set `prometheus.metrics.reporter.listener.enable` to `false` so the MirrorMaker metrics are automatically exposed via the listener of the Connect runtime.

For example:
```json
{
    "name": "source",
    "connector.class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    ...

    "metric.reporters": "io.strimzi.kafka.metrics.prometheus.ClientMetricsReporter",
    "prometheus.metrics.reporter.listener.enable": "false"
}
```

## Accessing Metrics

Metrics are exposed on the configured listener on the `GET /metrics` endpoint. For example, by default this is `http://localhost:8080/metrics`.

## Getting help

If you encounter any issues while using Strimzi, you can get help using:

- [#strimzi channel on CNCF Slack](https://slack.cncf.io/)
- [Strimzi Users mailing list](https://lists.cncf.io/g/cncf-strimzi-users/topics)
- [GitHub Discussions](https://github.com/orgs/strimzi/discussions)

## Strimzi Community Meetings

You can join our regular community meetings:
* Thursday 8:00 AM UTC (every 4 weeks starting from 4th June 2020) - [convert to your timezone](https://www.thetimezoneconverter.com/?t=8%3A00&tz=UTC)
* Thursday 4:00 PM UTC (every 4 weeks starting from 18th June 2020) - [convert to your timezone](https://www.thetimezoneconverter.com/?t=16%3A00&tz=UTC)

Resources:
* [Meeting minutes, agenda and Zoom link](https://docs.google.com/document/d/1V1lMeMwn6d2x1LKxyydhjo2c_IFANveelLD880A6bYc/edit#heading=h.vgkvn1hr5uor)
* [Recordings](https://youtube.com/playlist?list=PLpI4X8PMthYfONZopcRd4X_stq1C14Rtn)
* [Calendar](https://calendar.google.com/calendar/embed?src=c_m9pusj5ce1b4hr8c92hsq50i00%40group.calendar.google.com) ([Subscribe to the calendar](https://calendar.google.com/calendar/u/0?cid=Y19tOXB1c2o1Y2UxYjRocjhjOTJoc3E1MGkwMEBncm91cC5jYWxlbmRhci5nb29nbGUuY29t))

## Contributing

You can contribute by:
- Raising any issues you find using Strimzi
- Fixing issues by opening Pull Requests
- Improving documentation
- Talking about Strimzi

All bugs, tasks or enhancements are tracked as [GitHub issues](https://github.com/strimzi/metrics-reporter/issues).

If you want to get in touch with us first before contributing, you can use:

- [#strimzi channel on CNCF Slack](https://slack.cncf.io/)
- [Strimzi Dev mailing list](https://lists.cncf.io/g/cncf-strimzi-dev/topics)
- [GitHub Discussions](https://github.com/orgs/strimzi/discussions)

## License
Strimzi is licensed under the [Apache License](./LICENSE), Version 2.0