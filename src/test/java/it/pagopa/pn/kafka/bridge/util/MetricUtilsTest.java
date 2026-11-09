package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricUtilsTest {
    @Test
    void generateGeneralMetricWithOnlyMetricNameAndMetricValueSetsAllFieldsCorrectly() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.PAYMENT_EVENT_AGE,
                3,
                null,
                MetricUtils.MetricUnit.SECONDS
        );

        assertEquals("pn-kafka-bridge", metric.getNamespace());
        assertEquals(1, metric.getMetrics().size());
        assertEquals(MetricUtils.MetricName.PAYMENT_EVENT_AGE.getValue(), metric.getMetrics().getFirst().getName());
        assertEquals(3, metric.getMetrics().getFirst().getValue());
        assertEquals(MetricUtils.MetricUnit.SECONDS.getValue(), metric.getUnit());
    }
}