package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

public final class MetricUtils {
    private static final String METRIC_NAMESPACE = "pn-kafka-bridge";

    private MetricUtils() {}

    @Getter
    @RequiredArgsConstructor
    public enum MetricName {
        PAYMENT_EVENT_AGE("payment_event_age");
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum MetricUnit {
        SECONDS("Seconds");
        private final String value;
    }

    public static GeneralMetric generateGeneralMetric(
        MetricName metricName,
        int metricValue,
        List<Dimension> dimensions,
        MetricUnit unit
    ) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setDimensions(dimensions);
        generalMetric.setTimestamp(Instant.now().toEpochMilli());
        generalMetric.setUnit(unit.getValue());
        return generalMetric;
    }
}
