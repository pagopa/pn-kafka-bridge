package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsErrorEventProducer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsNoticePaymentProducer;
import it.pagopa.pn.kafka.bridge.service.PagoPaPaymentServiceImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static it.pagopa.pn.kafka.bridge.middleware.kafka.consumer.PaymentFlowFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Timeout(20)
@ExtendWith(MockitoExtension.class)
class PagoPaPaymentFlowTest {

    private static final String ERROR_QUEUE = "payment-errors";
    private static final String PAYMENT_QUEUE = "notice-payments";
    private static final String QUEUE_URL_PREFIX = "https://sqs.eu-south-1.amazonaws.com/123456789012/";

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PagoPaPaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PagoPaPaymentConsumer(new PagoPaPaymentServiceImpl(
                new SqsErrorEventProducer(sqsClient, ERROR_QUEUE, objectMapper),
                new SqsNoticePaymentProducer(sqsClient, PAYMENT_QUEUE, objectMapper),
                objectMapper, new PagoPaPaymentMapper()), objectMapper);
    }

    @Test
    void routesMixedRecordsToTheirQueuesWithMappedPayloadsAndHeaders() throws Exception {
        successfulSqs();
        String malformed = "{invalid-json";
        Instant before = Instant.now();

        consumer.listen(TOPIC, List.of(record(0, validPayment(0)), record(1, malformed),
                record(2, validPayment(2))));

        Instant after = Instant.now();
        List<SendMessageBatchRequest> requests = requests(2);
        SendMessageBatchRequest errorRequest = requestFor(requests, ERROR_QUEUE);
        assertEquals(1, errorRequest.entries().size());
        PnErrorEvent.Payload error = objectMapper.readValue(
                errorRequest.entries().get(0).messageBody(), PnErrorEvent.Payload.class);
        assertEquals(malformed, error.getOriginalMessage());
        assertEquals("Error: message malformed.", error.getDescription());
        assertBetween(before, after, error.getTimestamp());
        assertHeaders(errorRequest, EventType.ERROR_EVENT);

        SendMessageBatchRequest paymentRequest = requestFor(requests, PAYMENT_QUEUE);
        assertEquals(2, paymentRequest.entries().size());
        PnNoticePaymentEvent.Payload first = payment(paymentRequest.entries().get(0));
        assertEquals("payment-0", first.getPaymentId());
        assertEquals("1", first.getModelType());
        assertEquals("iuv-0", first.getIuv());
        assertEquals("iur-0", first.getIur());
        assertEquals("notice-0", first.getNoticeCode());
        assertEquals("12345678901", first.getCreditorTaxId());
        assertEquals("Test creditor", first.getCreditorCompanyName());
        assertEquals(PAYMENT_TIMESTAMP, first.getPaymentDateTime());
        assertEquals(Instant.parse("2026-10-21T00:00:00Z"), first.getDueDate());
        assertEquals(Integer.valueOf(2), first.getTotalNotice());
        assertEquals("PSP", first.getPaymentSourceChannel());
        assertEquals(Integer.valueOf(1000), first.getAmount());
        assertEquals("50", first.getFee());
        assertEquals(PUBLISHED_TIMESTAMP, first.getPagopaPublishedTimestamp());
        assertBetween(before, after, first.getSendIngestionTimestamp());
        PnNoticePaymentEvent.Payload second = payment(paymentRequest.entries().get(1));
        assertEquals("payment-2", second.getPaymentId());
        assertEquals(PUBLISHED_TIMESTAMP.plusMillis(2), second.getPagopaPublishedTimestamp());
        assertEquals(first.getSendIngestionTimestamp(), second.getSendIngestionTimestamp());
        assertHeaders(paymentRequest, EventType.NOTICE_PAYMENT);
    }

    @Test
    void sendsTwentyThreeValidPaymentsInBatchesOfTenTenAndThree() throws Exception {
        successfulSqs();

        consumer.listen(TOPIC, IntStream.range(0, 23)
                .mapToObj(index -> record(index, validPayment(index))).toList());

        List<SendMessageBatchRequest> requests = requests(3);
        assertEquals(List.of(3, 10, 10), requests.stream().map(request -> request.entries().size()).sorted().toList());
        assertTrue(requests.stream().allMatch(request -> request.queueUrl().equals(QUEUE_URL_PREFIX + PAYMENT_QUEUE)));
        List<String> actualIds = new ArrayList<>();
        for (SendMessageBatchRequest request : requests) {
            for (SendMessageBatchRequestEntry entry : request.entries()) {
                actualIds.add(payment(entry).getPaymentId());
            }
        }
        assertEquals(IntStream.range(0, 23).mapToObj(index -> "payment-" + index).sorted().toList(),
                actualIds.stream().sorted().toList());
    }

    @Test
    void sendsTwentyThreeMalformedMessagesInBatchesOfTenTenAndThree() throws Exception {
        successfulSqs();
        List<String> bodies = IntStream.range(0, 23).mapToObj(index -> "{invalid-" + index).toList();

        consumer.listen(TOPIC, IntStream.range(0, bodies.size())
                .mapToObj(index -> record(index, bodies.get(index))).toList());

        List<SendMessageBatchRequest> requests = requests(3);
        assertEquals(List.of(3, 10, 10), requests.stream().map(request -> request.entries().size()).sorted().toList());
        assertTrue(requests.stream().allMatch(request -> request.queueUrl().equals(QUEUE_URL_PREFIX + ERROR_QUEUE)));
        List<String> originals = new ArrayList<>();
        for (SendMessageBatchRequest request : requests) {
            for (SendMessageBatchRequestEntry entry : request.entries()) {
                originals.add(objectMapper.readValue(entry.messageBody(), PnErrorEvent.Payload.class).getOriginalMessage());
            }
        }
        assertEquals(bodies.stream().sorted().toList(), originals.stream().sorted().toList());
    }

    @Test
    void splitsMalformedMessagesByActualUtf8RequestSize() throws Exception {
        successfulSqs();
        String malformed = "\u00e8".repeat(300_000);

        consumer.listen(TOPIC, List.of(record(0, malformed), record(1, malformed)));

        for (SendMessageBatchRequest request : requests(2)) {
            assertEquals(QUEUE_URL_PREFIX + ERROR_QUEUE, request.queueUrl());
            assertEquals(1, request.entries().size());
            assertTrue(requestBytes(request) <= MAX_BATCH_BYTES);
            assertEquals(malformed, objectMapper.readValue(
                    request.entries().get(0).messageBody(), PnErrorEvent.Payload.class).getOriginalMessage());
        }
    }

    @Test
    void skipsOversizedMalformedMessageButStillSendsOtherRecords() throws Exception {
        successfulSqs();

        consumer.listen(TOPIC, List.of(record(0, "x".repeat(MAX_BATCH_BYTES)),
                record(1, "{invalid"), record(2, validPayment(2))));

        List<SendMessageBatchRequest> requests = requests(2);
        SendMessageBatchRequest errorRequest = requestFor(requests, ERROR_QUEUE);
        assertEquals(1, errorRequest.entries().size());
        assertEquals("{invalid", objectMapper.readValue(
                errorRequest.entries().get(0).messageBody(), PnErrorEvent.Payload.class).getOriginalMessage());
        assertEquals("payment-2", payment(requestFor(requests, PAYMENT_QUEUE).entries().get(0)).getPaymentId());
    }

    @Test
    void emptyPollDoesNotAccessSqs() {
        consumer.listen(TOPIC, List.of());

        verifyNoInteractions(sqsClient);
    }

    @Test
    void onlyOversizedMalformedMessagesDoNotAccessSqs() {
        consumer.listen(TOPIC, List.of(record(0, "x".repeat(MAX_BATCH_BYTES))));

        verifyNoInteractions(sqsClient);
    }

    @Test
    void missingKafkaTimestampFailsBeforeSending() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(TOPIC, 0, 0, -1,
                TimestampType.NO_TIMESTAMP_TYPE, -1, -1, "key", validPayment(0), new RecordHeaders(), Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> consumer.listen(TOPIC, List.of(record)));

        verifyNoInteractions(sqsClient);
    }

    @Test
    void invalidNumericPaymentFieldFailsBeforeSendingValidPaymentBatches() {
        String invalid = validPayment(1).replace("\"totalNotice\": \"2\"", "\"totalNotice\": \"invalid\"");

        assertThrows(NumberFormatException.class,
                () -> consumer.listen(TOPIC, List.of(record(0, validPayment(0)), record(1, invalid))));

        verifyNoInteractions(sqsClient);
    }

    @Test
    void propagatesSqsFailureToCaller() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL_PREFIX + PAYMENT_QUEUE).build());
        var failure = SqsException.builder().message("SQS unavailable").statusCode(503).build();
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(SqsException.class,
                () -> consumer.listen(TOPIC, List.of(record(0, validPayment(0))))));
        verify(sqsClient).sendMessageBatch(any(SendMessageBatchRequest.class));
    }

    @Test
    void errorQueueFailurePreventsSendingValidPayments() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL_PREFIX + ERROR_QUEUE).build());
        var failure = SqsException.builder().message("Error queue unavailable").statusCode(503).build();
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(SqsException.class,
                () -> consumer.listen(TOPIC, List.of(record(0, "{invalid"), record(1, validPayment(1))))));

        assertEquals(QUEUE_URL_PREFIX + ERROR_QUEUE, requests(1).get(0).queueUrl());
        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
    }

    private void successfulSqs() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenAnswer(invocation -> {
            GetQueueUrlRequest request = invocation.getArgument(0);
            return GetQueueUrlResponse.builder().queueUrl(QUEUE_URL_PREFIX + request.queueName()).build();
        });
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class))).thenAnswer(invocation -> {
            SendMessageBatchRequest request = invocation.getArgument(0);
            return SendMessageBatchResponse.builder().successful(request.entries().stream()
                    .map(entry -> SendMessageBatchResultEntry.builder().id(entry.id()).messageId("sqs-message").build())
                    .toList()).build();
        });
    }

    private List<SendMessageBatchRequest> requests(int count) {
        ArgumentCaptor<SendMessageBatchRequest> captor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(sqsClient, times(count)).sendMessageBatch(captor.capture());
        return captor.getAllValues();
    }

    private SendMessageBatchRequest requestFor(List<SendMessageBatchRequest> requests, String queue) {
        return requests.stream().filter(request -> request.queueUrl().equals(QUEUE_URL_PREFIX + queue))
                .findFirst().orElseThrow();
    }

    private PnNoticePaymentEvent.Payload payment(SendMessageBatchRequestEntry entry) throws Exception {
        return objectMapper.readValue(entry.messageBody(), PnNoticePaymentEvent.Payload.class);
    }

    private void assertHeaders(SendMessageBatchRequest request, EventType type) {
        request.entries().forEach(entry -> {
            assertEquals(type.name(), entry.messageAttributes().get("eventType").stringValue());
            assertEquals(EventPublisher.KAFKA_BRIDGE.name(), entry.messageAttributes().get("publisher").stringValue());
            assertNotNull(Instant.parse(entry.messageAttributes().get("createdAt").stringValue()));
        });
    }

    private void assertBetween(Instant before, Instant after, Instant actual) {
        assertNotNull(actual);
        assertFalse(actual.isBefore(before));
        assertFalse(actual.isAfter(after));
    }

    private int requestBytes(SendMessageBatchRequest request) {
        return request.entries().stream().mapToInt(entry -> entry.messageBody().getBytes(StandardCharsets.UTF_8).length
                + entry.messageAttributes().entrySet().stream().mapToInt(attribute ->
                        attribute.getKey().getBytes(StandardCharsets.UTF_8).length
                                + attribute.getValue().dataType().getBytes(StandardCharsets.UTF_8).length
                                + (attribute.getValue().stringValue() == null ? 0
                                : attribute.getValue().stringValue().getBytes(StandardCharsets.UTF_8).length)).sum())
                .sum();
    }
}
