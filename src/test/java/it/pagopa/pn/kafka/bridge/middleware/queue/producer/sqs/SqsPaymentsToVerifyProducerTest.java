package it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsPaymentsToVerifyProducerTest {

    private static final String QUEUE_NAME = "payments-to-verify";
    private static final String QUEUE_URL = "https://sqs.eu-south-1.amazonaws.com/123456789012/" + QUEUE_NAME;

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private SqsPaymentsToVerifyProducer producer;
    private PnNoticePaymentEvent event;

    @BeforeEach
    void setUp() {
        producer = new SqsPaymentsToVerifyProducer(sqsClient, QUEUE_NAME, objectMapper);
        event = PnNoticePaymentEvent.builder()
                .header(StandardEventHeader.builder()
                        .eventId("event-id")
                        .eventType(EventType.NOTICE_PAYMENT.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .createdAt(Instant.parse("2026-10-20T08:00:00Z"))
                        .build())
                .payload(PnNoticePaymentEvent.Payload.builder()
                        .paymentId("payment-id")
                        .noticeCode("notice-code")
                        .sendIngestionTimestamp(Instant.parse("2026-10-20T08:01:00Z"))
                        .build())
                .build();
    }

    @Test
    void sendsSerializedPayloadAndEventHeadersToConfiguredQueue() throws Exception {
        queueExists();

        producer.push(event);

        ArgumentCaptor<SendMessageRequest> request = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build());
        verify(sqsClient).sendMessage(request.capture());
        verifyNoMoreInteractions(sqsClient);
        assertEquals(QUEUE_URL, request.getValue().queueUrl());
        assertEquals(objectMapper.readTree(objectMapper.writeValueAsString(event.getPayload())),
                objectMapper.readTree(request.getValue().messageBody()));
        assertEquals("event-id", request.getValue().messageAttributes().get("eventId").stringValue());
        assertEquals(EventType.NOTICE_PAYMENT.name(),
                request.getValue().messageAttributes().get("eventType").stringValue());
        assertEquals(EventPublisher.KAFKA_BRIDGE.name(),
                request.getValue().messageAttributes().get("publisher").stringValue());
        assertEquals(event.getHeader().getCreatedAt().toString(),
                request.getValue().messageAttributes().get("createdAt").stringValue());
        assertTrue(request.getValue().messageAttributes().values().stream()
                .allMatch(attribute -> "String".equals(attribute.dataType())));
    }

    @Test
    void propagatesQueueResolutionFailureWithoutSending() {
        var failure = SqsException.builder().message("Queue unavailable").statusCode(503).build();
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(SqsException.class, () -> producer.push(event)));

        verify(sqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
        verifyNoMoreInteractions(sqsClient);
    }

    @Test
    void propagatesSendFailure() {
        queueExists();
        var failure = SqsException.builder().message("Send failed").statusCode(503).build();
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(SqsException.class, () -> producer.push(event)));

        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void preservesSerializationFailureAndDoesNotSend() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        ObjectWriter writer = mock(ObjectWriter.class);
        JsonProcessingException failure = new JsonProcessingException("Cannot serialize payment") { };
        when(failingMapper.writerFor(PnNoticePaymentEvent.Payload.class)).thenReturn(writer);
        when(writer.writeValueAsString(event.getPayload())).thenThrow(failure);
        producer = new SqsPaymentsToVerifyProducer(sqsClient, QUEUE_NAME, failingMapper);
        queueExists();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> producer.push(event));

        assertSame(failure, thrown.getCause());
        verify(sqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
        verifyNoMoreInteractions(sqsClient);
    }

    private void queueExists() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build());
    }
}
