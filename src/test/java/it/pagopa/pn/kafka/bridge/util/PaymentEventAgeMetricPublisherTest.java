package it.pagopa.pn.kafka.bridge.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventAgeMetricPublisherTest {

    @Mock
    private KafkaPaymentEventAgeCache cache;

    @InjectMocks
    private PaymentEventAgeMetricPublisher publisher;

    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(
                PaymentEventAgeMetricPublisher.class
        );

        logAppender = new ListAppender<>();
        logAppender.start();

        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void shouldNotPublishMetricWhenCacheIsEmpty() {
        when(cache.drain()).thenReturn(Map.of());

        publisher.publishMetricsAndClearCache();

        Assertions.assertTrue(logAppender.list.isEmpty());
        verify(cache).drain();
    }

    @Test
    void shouldPublishMetricWithMaximumAge() {
        when(cache.drain()).thenReturn(Map.of(
                0, 3000L,
                1, 12000L,
                2, 7000L
        ));

        publisher.publishMetricsAndClearCache();

        ILoggingEvent event = getMetricEvent();

        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "Max age in kafka payment events recorded is 12s for partition=1",
                event.getFormattedMessage()
        );

        verify(cache).drain();
    }

    @Test
    void shouldConvertMillisecondsToSeconds() {
        when(cache.drain()).thenReturn(Map.of(5, 12500L));

        publisher.publishMetricsAndClearCache();

        ILoggingEvent event = getMetricEvent();

        assertEquals(
                "Max age in kafka payment events recorded is 12s for partition=5",
                event.getFormattedMessage()
        );
    }

    @Test
    void shouldLogFatalWhenMoreThan1000Partitions() {
        Map<Integer, Long> drained = new HashMap<>();

        for (int i = 0; i < 1001; i++) {
            drained.put(i, (long) i * 1000);
        }

        when(cache.drain()).thenReturn(drained);

        publisher.publishMetricsAndClearCache();

        Assertions.assertTrue(logAppender.list.stream().anyMatch(event ->
                event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains(
                        "Too many partitions in the cache"
                )
        ));

        Assertions.assertTrue(logAppender.list.stream().anyMatch(event ->
                event.getLevel() == Level.INFO
                        && event.getFormattedMessage()
                        .contains("Max age in kafka payment events")
        ));

        assertEquals(2, logAppender.list.size());

        verify(cache).drain();
    }

    private ILoggingEvent getMetricEvent() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("Metric log not found")
                );
    }
}