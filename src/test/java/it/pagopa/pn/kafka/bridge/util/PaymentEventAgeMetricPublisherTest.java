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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventAgeMetricPublisherTest {
    private static final int EMPTY_WINDOWS_THRESHOLD = 3;

    @Mock
    private KafkaPaymentEventAgeCache cache;

    private PaymentEventAgeMetricPublisher publisher;

    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {

        publisher = new PaymentEventAgeMetricPublisher(cache, EMPTY_WINDOWS_THRESHOLD);
        logger = (Logger) LoggerFactory.getLogger(PaymentEventAgeMetricPublisher.class);
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
    void shouldPublishOnlyAgeMetricWithDefaultValueWhenWindowIsEmptyBelowThreshold() {
        when(cache.drain()).thenReturn(Map.of());

        publisher.drainCacheAndPublishMetrics();

        assertEquals(
                List.of("No events received in the last window. Consecutive empty windows: 1"),
                getLoggedMessages()
        );
        verify(cache).drain();
    }

    @Test
    void shouldPublishConsecutiveEmptyWindowsMetricWhenThresholdIsReached() {
        when(cache.drain()).thenReturn(Map.of());

        for (int i = 0; i < EMPTY_WINDOWS_THRESHOLD; i++) {
            publisher.drainCacheAndPublishMetrics();
        }

        List<String> messages = getLoggedMessages();

        // 3 metriche age (una per finestra) + 1 metrica contatore (solo alla terza)
        assertEquals(EMPTY_WINDOWS_THRESHOLD + 1, messages.size());
        assertEquals(
                "No events received in the last window. Consecutive empty windows: 3",
                messages.get(EMPTY_WINDOWS_THRESHOLD - 1)
        );
        assertEquals(
                "No payment events consumed in the last window (consecutive empty windows: 3, threshold: 3)",
                messages.get(EMPTY_WINDOWS_THRESHOLD)
        );
        verify(cache, times(EMPTY_WINDOWS_THRESHOLD)).drain();
    }

    @Test
    void shouldKeepPublishingConsecutiveEmptyWindowsMetricAfterThresholdIsExceeded() {
        when(cache.drain()).thenReturn(Map.of());

        for (int i = 0; i < EMPTY_WINDOWS_THRESHOLD + 1; i++) {
            publisher.drainCacheAndPublishMetrics();
        }

        List<String> messages = getLoggedMessages();

        // 4 metriche age + 2 metriche contatore (terza e quarta finestra)
        assertEquals(6, messages.size());
        assertEquals(
                "No payment events consumed in the last window (consecutive empty windows: 4, threshold: 3)",
                messages.getLast()
        );
    }

    @Test
    void shouldPublishMetricWithMaximumAge() {
        when(cache.drain()).thenReturn(Map.of(
                0, 3000L,
                1, 12500L,
                2, 7000L
        ));

        publisher.drainCacheAndPublishMetrics();

        // 12500 ms viene troncato a 12 s
        assertEquals(
                List.of("Max age in kafka payment events is 12s for partition=1"),
                getLoggedMessages()
        );
        verify(cache).drain();
    }

    @Test
    void shouldResetConsecutiveEmptyWindowsWhenEventsAreReceived() {
        when(cache.drain()).thenReturn(
                Map.of(),               // vuota (1)
                Map.of(),               // vuota (2)
                Map.of(0, 5000L),       // eventi: reset del contatore
                Map.of(),               // vuota (1)
                Map.of()                // vuota (2)
        );

        for (int i = 0; i < 5; i++) {
            publisher.drainCacheAndPublishMetrics();
        }

        List<String> messages = getLoggedMessages();

        // Senza reset il contatore avrebbe raggiunto la soglia alla quinta finestra
        Assertions.assertTrue(messages.stream().noneMatch(m -> m.contains("threshold:")));
        assertEquals(5, messages.size());
        assertEquals(
                "No events received in the last window. Consecutive empty windows: 2",
                messages.get(4)
        );
        verify(cache, times(5)).drain();
    }

    private List<String> getLoggedMessages() {
        logAppender.list.forEach(event -> assertEquals(Level.INFO, event.getLevel()));
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}