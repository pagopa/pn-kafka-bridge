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
        PAYMENT_EVENT_AGE("payment_event_age"),
        PAYMENT_EVENT_CONSECUTIVE_EMPTY_WINDOWS("payment_event_consecutive_empty_windows");
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum MetricUnit {
        SECONDS("Seconds"),
        COUNT("Count");
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
        if(unit != null) {
            generalMetric.setUnit(unit.getValue());
        }
        return generalMetric;
    }
}
