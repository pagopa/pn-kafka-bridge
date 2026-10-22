package it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs.handler.PagoPaPaymentMessageHandler;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsPaymentsToVerifyProducer;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.support.MessageBuilder;
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
class KafkaBridgePaymentsFlowTest {

    private static final String QUEUE_NAME = "payments-to-verify";
    private static final String QUEUE_URL = "https://sqs.eu-south-1.amazonaws.com/123456789012/" + QUEUE_NAME;

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private KafkaBridgePaymentsConsumer consumer;
    private PagoPaPaymentMessage payment;

    @BeforeEach
    void setUp() throws Exception {
        MDC.clear();
        consumer = new KafkaBridgePaymentsConsumer(new PagoPaPaymentMessageHandler(
                new SqsPaymentsToVerifyProducer(sqsClient, QUEUE_NAME, objectMapper), new PagoPaPaymentMapper()));
        payment = objectMapper.readValue("""
                {"id":"payment-id","timestamp":"2026-10-20T08:00:00Z",
                 "debtorPosition":{"noticeNumber":"notice-code"},
                 "creditor":{"idPA":"12345678901"},
                 "paymentInfo":{"totalNotice":"2","fee":50}}
                """, PagoPaPaymentMessage.class);
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build());
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void forwardsConsumedPaymentThroughHandlerToPaymentsToVerifyQueue() throws Exception {
        Instant before = Instant.now();

        consumer.consume(MessageBuilder.withPayload(payment).build());

        Instant after = Instant.now();
        ArgumentCaptor<SendMessageRequest> request = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build());
        verify(sqsClient).sendMessage(request.capture());
        verifyNoMoreInteractions(sqsClient);
        assertEquals(QUEUE_URL, request.getValue().queueUrl());
        assertEquals(EventType.NOTICE_PAYMENT.name(),
                request.getValue().messageAttributes().get("eventType").stringValue());
        PnNoticePaymentEvent.Payload payload = objectMapper.readValue(
                request.getValue().messageBody(), PnNoticePaymentEvent.Payload.class);
        assertEquals(payment.getId(), payload.getPaymentId());
        assertFalse(payload.getSendIngestionTimestamp().isBefore(before));
        assertFalse(payload.getSendIngestionTimestamp().isAfter(after));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
    }

    @Test
    void propagatesWrappedSqsFailureToConsumerCallerAndCleansTopic() {
        var failure = SqsException.builder().message("SQS unavailable").statusCode(503).build();
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(failure);

        PnInternalException thrown = assertThrows(PnInternalException.class,
                () -> consumer.consume(MessageBuilder.withPayload(payment).build()));

        assertSame(failure, thrown.getCause());
        assertEquals("PN_PAYMENT_EVENTFAILED", thrown.getProblem().getErrors().get(0).getCode());
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
    }
}
