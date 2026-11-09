package it.pagopa.pn.kafka.bridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MalformedPaymentBatchSizeTest {

    private static final int LIMIT = 1_048_576;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PagoPaPaymentServiceImpl service =
            new PagoPaPaymentServiceImpl(mock(), mock(), objectMapper, new PagoPaPaymentMapper());

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
