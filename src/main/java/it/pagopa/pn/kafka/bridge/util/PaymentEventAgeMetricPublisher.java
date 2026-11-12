package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@CustomLog
public class PaymentEventAgeMetricPublisher {
    private final KafkaPaymentEventAgeCache cache;
    private int consecutiveEmptyWindows;
    private final int emptyWindowsThreshold;

    public PaymentEventAgeMetricPublisher(
            KafkaPaymentEventAgeCache cache,
            @Value("${pn.kafka-bridge.metrics.payments-events.empty-windows-threshold:12}") int emptyWindowsThreshold
    ) {
        this.cache = cache;
        this.emptyWindowsThreshold = emptyWindowsThreshold;
        this.consecutiveEmptyWindows = 0;
    }

    @Scheduled(cron = "${pn.kafka-bridge.metrics.payments-events.cron}")
    public void drainCacheAndPublishMetrics() {
        Map<Integer, Long> maxAgeMsByPartition = cache.drain();

        if (maxAgeMsByPartition.isEmpty()) {
            handleEmptyWindow();
        } else {
            handleWindowWithEvents(maxAgeMsByPartition);
        }
    }

    private void handleEmptyWindow() {
        consecutiveEmptyWindows++;
        publishAgeMetric(-1,
                String.format("No events received in the last window. Consecutive empty windows: %d", consecutiveEmptyWindows));
        if(consecutiveEmptyWindows >= emptyWindowsThreshold) {
            publishConsecutiveEmptyWindowsMetric(
                    String.format("No payment events consumed in the last window (consecutive empty windows: %d, threshold: %d)",
                            consecutiveEmptyWindows, emptyWindowsThreshold));
        }
    }

    private void handleWindowWithEvents(Map<Integer, Long> maxAgeMsByPartition) {
        consecutiveEmptyWindows = 0;

        Map.Entry<Integer, Long> oldestPartitionEntry =
                Collections.max(maxAgeMsByPartition.entrySet(), Map.Entry.comparingByValue());
        int maxAgeSeconds = (int) (oldestPartitionEntry.getValue() / 1000);
        publishAgeMetric(maxAgeSeconds,
                String.format("Max age in kafka payment events is %ss for partition=%s",
                        maxAgeSeconds, oldestPartitionEntry.getKey()));
    }

    private void publishAgeMetric(int ageSeconds, String logMessage) {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.PAYMENT_EVENT_AGE,
                ageSeconds,
                List.of(),
                MetricUtils.MetricUnit.SECONDS);
        log.logMetric(List.of(metric), logMessage);
    }

    private void publishConsecutiveEmptyWindowsMetric(String logMessage) {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.PAYMENT_EVENT_CONSECUTIVE_EMPTY_WINDOWS,
                consecutiveEmptyWindows,
                List.of(),
                MetricUtils.MetricUnit.COUNT);
        log.logMetric(List.of(metric), logMessage);
    }
}
