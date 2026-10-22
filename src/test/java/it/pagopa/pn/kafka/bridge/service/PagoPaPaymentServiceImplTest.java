package it.pagopa.pn.kafka.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(10)
@ExtendWith(MockitoExtension.class)
class PagoPaPaymentServiceImplTest {

    private static final int MAX_BATCH_BYTES = 1_048_576;
    private static final Instant INGESTION_TIMESTAMP = Instant.parse("2026-10-20T08:00:00Z");

    @Mock
    private MomProducer<PnErrorEvent> errorProducer;
    @Mock
    private MomProducer<PnNoticePaymentEvent> paymentProducer;
    @Mock
    private PagoPaPaymentMapper mapper;
    @Captor
    private ArgumentCaptor<List<PnErrorEvent>> errorBatches;
    @Captor
    private ArgumentCaptor<List<PnNoticePaymentEvent>> paymentBatches;

    private PagoPaPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PagoPaPaymentServiceImpl(errorProducer, paymentProducer,
                new ObjectMapper().findAndRegisterModules(), mapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 11, 20, 23})
    void sendsMalformedEventsInBatchesOfAtMostTen(int count) {
        List<String> messages = IntStream.range(0, count).mapToObj(index -> "message-" + index).toList();
        List<PnErrorEvent> events = messages.stream().map(this::errorEvent).toList();
        when(mapper.buildMalformedEvents(messages)).thenReturn(events);

        service.sendMessage(messages);

        verify(mapper).buildMalformedEvents(messages);
        verify(errorProducer, times(Math.ceilDiv(count, 10))).push(errorBatches.capture());
        assertBatches(events, errorBatches.getAllValues());
        verifyNoInteractions(paymentProducer);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 11, 20, 23})
    void sendsPaymentEventsInBatchesOfAtMostTen(int count) {
        List<PagoPaPaymentMessage> messages = IntStream.range(0, count)
                .mapToObj(index -> new PagoPaPaymentMessage()).toList();
        List<PnNoticePaymentEvent> events = IntStream.range(0, count)
                .mapToObj(index -> PnNoticePaymentEvent.builder()
                        .payload(PnNoticePaymentEvent.Payload.builder().paymentId("payment-" + index).build())
                        .build()).toList();
        when(mapper.buildEvents(messages, INGESTION_TIMESTAMP)).thenReturn(events);

        service.sendMessage(messages, INGESTION_TIMESTAMP);

        verify(mapper).buildEvents(messages, INGESTION_TIMESTAMP);
        verify(paymentProducer, times(Math.ceilDiv(count, 10))).push(paymentBatches.capture());
        assertBatches(events, paymentBatches.getAllValues());
        verifyNoInteractions(errorProducer);
    }

    @Test
    void splitsMalformedBatchWhenUtf8SizeExceedsLimit() {
        List<String> messages = List.of("\u00e8".repeat(300_000), "\u00e8".repeat(300_000));
        List<PnErrorEvent> events = messages.stream().map(this::errorEvent).toList();
        when(mapper.buildMalformedEvents(messages)).thenReturn(events);

        service.sendMessage(messages);

        verify(errorProducer, times(2)).push(errorBatches.capture());
        assertTrue(errorBatches.getAllValues().stream().allMatch(batch -> batch.size() == 1));
        assertEquals(events, errorBatches.getAllValues().stream().flatMap(List::stream).toList());
        verifyNoInteractions(paymentProducer);
    }

    @Test
    void skipsOversizedEventsButSendsRemainingEvents() {
        List<String> messages = List.of("before", "x".repeat(MAX_BATCH_BYTES), "after");
        List<PnErrorEvent> events = messages.stream().map(this::errorEvent).toList();
        when(mapper.buildMalformedEvents(messages)).thenReturn(events);

        service.sendMessage(messages);

        verify(errorProducer).push(List.of(events.get(0), events.get(2)));
        verifyNoInteractions(paymentProducer);
    }

    @Test
    void doesNotCallProducersWhenAllEventsAreOversized() {
        List<String> messages = List.of("x".repeat(MAX_BATCH_BYTES));
        when(mapper.buildMalformedEvents(messages)).thenReturn(List.of(errorEvent(messages.get(0))));

        service.sendMessage(messages);

        verifyNoInteractions(errorProducer, paymentProducer);
    }

    @Test
    void propagatesSerializationFailureBeforeSendingAnyBatch() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        ObjectWriter writer = mock(ObjectWriter.class);
        JsonProcessingException failure = new JsonProcessingException("Cannot serialize payload") { };
        List<String> messages = List.of("first", "second");
        List<PnErrorEvent> events = messages.stream().map(this::errorEvent).toList();
        when(mapper.buildMalformedEvents(messages)).thenReturn(events);
        when(failingMapper.writerFor(PnErrorEvent.Payload.class)).thenReturn(writer);
        when(writer.writeValueAsString(events.get(0).getPayload())).thenReturn("{}");
        when(writer.writeValueAsString(events.get(1).getPayload())).thenThrow(failure);
        service = new PagoPaPaymentServiceImpl(errorProducer, paymentProducer, failingMapper, mapper);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.sendMessage(messages));

        assertEquals("Cannot serialize malformed event for SQS size validation", thrown.getMessage());
        assertSame(failure, thrown.getCause());
        verifyNoInteractions(errorProducer, paymentProducer);
    }

    @Test
    void propagatesMalformedProducerFailure() {
        List<String> messages = List.of("message");
        List<PnErrorEvent> events = List.of(errorEvent("message"));
        when(mapper.buildMalformedEvents(messages)).thenReturn(events);
        IllegalStateException failure = new IllegalStateException("Error queue unavailable");
        doThrow(failure).when(errorProducer).push(events);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.sendMessage(messages)));

        verifyNoInteractions(paymentProducer);
    }

    @Test
    void propagatesPaymentProducerFailure() {
        List<PagoPaPaymentMessage> messages = List.of(new PagoPaPaymentMessage());
        List<PnNoticePaymentEvent> events = List.of(PnNoticePaymentEvent.builder().build());
        when(mapper.buildEvents(messages, INGESTION_TIMESTAMP)).thenReturn(events);
        IllegalStateException failure = new IllegalStateException("Payment queue unavailable");
        doThrow(failure).when(paymentProducer).push(events);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.sendMessage(messages, INGESTION_TIMESTAMP)));

        verifyNoInteractions(errorProducer);
    }

    @Test
    void propagatesMappingFailureWithoutSending() {
        List<PagoPaPaymentMessage> messages = List.of(new PagoPaPaymentMessage());
        IllegalArgumentException failure = new IllegalArgumentException("Invalid payment");
        when(mapper.buildEvents(messages, INGESTION_TIMESTAMP)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> service.sendMessage(messages, INGESTION_TIMESTAMP)));

        verifyNoInteractions(errorProducer, paymentProducer);
    }

    private PnErrorEvent errorEvent(String message) {
        return PnErrorEvent.builder()
                .header(StandardEventHeader.builder().createdAt(Instant.EPOCH)
                        .eventType("ERROR_EVENT").publisher("KAFKA_BRIDGE").build())
                .payload(PnErrorEvent.Payload.builder().originalMessage(message).timestamp(Instant.EPOCH).build())
                .build();
    }

    private <T> void assertBatches(List<T> events, List<List<T>> batches) {
        // Parallel sends may reorder batches, but not events within a batch.
        List<List<T>> ordered = batches.stream()
                .sorted(java.util.Comparator.comparingInt(batch -> events.indexOf(batch.get(0))))
                .toList();
        assertEquals(IntStream.iterate(0, start -> start < events.size(), start -> start + 10)
                        .map(start -> Math.min(10, events.size() - start)).boxed().toList(),
                ordered.stream().map(List::size).toList());
        assertEquals(events, ordered.stream().flatMap(List::stream).toList());
    }
}
