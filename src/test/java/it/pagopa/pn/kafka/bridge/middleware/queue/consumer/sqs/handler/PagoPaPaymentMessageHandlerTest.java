package it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs.handler;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentMessageHandlerTest {

    @Mock
    private MomProducer<PnNoticePaymentEvent> producer;
    @Mock
    private PagoPaPaymentMapper mapper;

    private PagoPaPaymentMessageHandler handler;
    private final PagoPaPaymentMessage payment = new PagoPaPaymentMessage();

    @BeforeEach
    void setUp() {
        handler = new PagoPaPaymentMessageHandler(producer, mapper);
    }

    @Test
    void mapsWithCurrentIngestionTimestampAndPushesTheMappedEvent() {
        PnNoticePaymentEvent event = PnNoticePaymentEvent.builder().build();
        when(mapper.toPnNoticePaymentEvent(same(payment), any(Instant.class))).thenReturn(event);
        Instant before = Instant.now();

        handler.handleEvent(payment);

        Instant after = Instant.now();
        ArgumentCaptor<Instant> timestamp = ArgumentCaptor.forClass(Instant.class);
        var order = inOrder(mapper, producer);
        order.verify(mapper).toPnNoticePaymentEvent(same(payment), timestamp.capture());
        order.verify(producer).push(same(event));
        assertFalse(timestamp.getValue().isBefore(before));
        assertFalse(timestamp.getValue().isAfter(after));
        verifyNoMoreInteractions(mapper, producer);
    }

    @Test
    void wrapsMappingFailureWithoutCallingProducer() {
        RuntimeException failure = new IllegalArgumentException("Invalid payment");
        when(mapper.toPnNoticePaymentEvent(same(payment), any(Instant.class))).thenThrow(failure);

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> handler.handleEvent(payment));

        assertPaymentFailure(thrown, failure);
        verifyNoInteractions(producer);
    }

    @Test
    void wrapsProducerFailurePreservingItsCause() {
        PnNoticePaymentEvent event = PnNoticePaymentEvent.builder().build();
        RuntimeException failure = new IllegalStateException("SQS unavailable");
        when(mapper.toPnNoticePaymentEvent(same(payment), any(Instant.class))).thenReturn(event);
        doThrow(failure).when(producer).push(event);

        PnInternalException thrown = assertThrows(PnInternalException.class, () -> handler.handleEvent(payment));

        assertPaymentFailure(thrown, failure);
        verify(producer).push(same(event));
        verifyNoMoreInteractions(producer);
    }

    private void assertPaymentFailure(PnInternalException thrown, RuntimeException cause) {
        assertSame(cause, thrown.getCause());
        assertEquals("Error handling payment message", thrown.getProblem().getDetail());
        assertEquals(500, thrown.getProblem().getStatus());
        assertEquals("PN_PAYMENT_EVENTFAILED", thrown.getProblem().getErrors().get(0).getCode());
    }
}
