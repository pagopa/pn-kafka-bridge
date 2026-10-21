package it.pagopa.pn.kafka.bridge.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MalformedPaymentBatchSizeTest {

    private static final int LIMIT = 1_048_576;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PagoPaPaymentServiceImpl service =
            new PagoPaPaymentServiceImpl(mock(), mock(), objectMapper, new PagoPaPaymentMapper());

    @Test
    void acceptsExactLimitAndSkipsOneByteOverWithTruncatedFatalLog() throws Exception {
        int overhead = objectMapper.writerFor(PnErrorEvent.Payload.class)
                .writeValueAsString(event("").getPayload()).getBytes(StandardCharsets.UTF_8).length + 112;
        PnErrorEvent exact = event("a".repeat(LIMIT - overhead));
        PnErrorEvent oneByteOver = event("a".repeat(LIMIT - overhead + 1));
        String prefix = "\uD83D\uDE00".repeat(256);
        PnErrorEvent oversized = event(prefix + "b".repeat(LIMIT));
        Logger logger = (Logger) LoggerFactory.getLogger(PagoPaPaymentServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals(List.of(List.of(exact), List.of(event("after"))),
                    service.buildMalformedBatches(List.of(exact, oneByteOver, oversized, event("after"))));
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("1048577 bytes"));
            ILoggingEvent log = appender.list.get(1);
            assertEquals(Level.ERROR, log.getLevel());
            assertEquals("* FATAL * ALLARM!", log.getMarkerList().get(0).getName());
            assertTrue(log.getFormattedMessage().endsWith(prefix));
            assertFalse(log.getFormattedMessage().contains("bbbb"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void splitsByUtf8SizeEvenWithFewerThanTenEvents() {
        PnErrorEvent first = event("\u00e8".repeat(300_000));
        PnErrorEvent second = event("\u00e8".repeat(300_000));

        assertEquals(List.of(List.of(first), List.of(second)),
                service.buildMalformedBatches(List.of(first, second)));
    }

    @Test
    void includesJsonEscapingInSizeCheck() {
        PnErrorEvent first = event("\"".repeat(300_000));
        PnErrorEvent second = event("\\".repeat(300_000));

        assertEquals(List.of(List.of(first), List.of(second)),
                service.buildMalformedBatches(List.of(first, second)));
    }

    @Test
    void returnsNoBatchesWhenEveryEventIsOversized() {
        assertTrue(service.buildMalformedBatches(List.of(event("a".repeat(LIMIT)))).isEmpty());
    }

    private PnErrorEvent event(String message) {
        return PnErrorEvent.builder()
                .header(StandardEventHeader.builder()
                        .eventId("id").eventType("ERROR_EVENT").createdAt(Instant.EPOCH)
                        .publisher("publisher").iun("iun").build())
                .payload(PnErrorEvent.Payload.builder()
                        .originalMessage(message).timestamp(Instant.EPOCH)
                        .description("Error: message malformed.").build())
                .build();
    }
}
