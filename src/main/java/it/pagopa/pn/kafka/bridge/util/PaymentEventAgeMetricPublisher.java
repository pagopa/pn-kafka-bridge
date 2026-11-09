package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@CustomLog
public class PaymentEventAgeMetricPublisher {
    private final KafkaPaymentEventAgeCache cache;

    @Scheduled(cron = "${pn.kafka-bridge.metrics.payments-events.cron}")
    public void publishMetricsAndClearCache() {
        Map<Integer, Long> drained = cache.drain();
        if (drained.isEmpty()) {
            log.debug("No payment events consumed in the last window, no metric published");
            return;
        }

        if(drained.size() > 1000) {
            log.fatal("Too many partitions in the cache, something is wrong. Cache size: {}", drained.size());
        }

        Map.Entry<Integer, Long> max = Collections.max(drained.entrySet(), Map.Entry.comparingByValue());
        int ageSeconds = (int) (max.getValue() / 1000);

        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.PAYMENT_EVENT_AGE,
                ageSeconds,
                List.of(),
                MetricUtils.MetricUnit.SECONDS
        );

        String msg = String.format("Max age in kafka payment events recorded is %ss for partition=%s", ageSeconds, max.getKey());
        log.logMetric(List.of(metric), msg);
    }
}
