package it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs.handler.PagoPaPaymentMessageHandler;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.util.MdcKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaBridgePaymentsConsumerTest {

    @Mock
    private PagoPaPaymentMessageHandler handler;

    private KafkaBridgePaymentsConsumer consumer;

    @BeforeEach
    void setUp() {
        MDC.clear();
        consumer = new KafkaBridgePaymentsConsumer(handler);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void delegatesPayloadWithMessageContextAndRemovesTopicAfterSuccess() {
        PagoPaPaymentMessage payment = new PagoPaPaymentMessage();
        Message<PagoPaPaymentMessage> message = MessageBuilder.withPayload(payment)
                .setHeader("aws_messageId", "message-id")
                .setHeader("X-Amzn-Trace-Id", "trace-id")
                .setHeader("iun", "notification-iun")
                .build();
        doAnswer(invocation -> {
            assertSame(payment, invocation.getArgument(0));
            assertEquals(MdcKey.PAYMENTS_KEY, MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
            assertEquals("message-id", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
            assertEquals("trace-id", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
            assertEquals("notification-iun", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
            return null;
        }).when(handler).handleEvent(payment);

        consumer.consume(message);

        verify(handler).handleEvent(payment);
        verifyNoMoreInteractions(handler);
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
    }

    @Test
    void propagatesHandlerFailureAndRemovesTopic() {
        PagoPaPaymentMessage payment = new PagoPaPaymentMessage();
        RuntimeException failure = new IllegalStateException("Cannot handle payment");
        doAnswer(invocation -> {
            assertEquals(MdcKey.PAYMENTS_KEY, MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
            throw failure;
        }).when(handler).handleEvent(payment);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> consumer.consume(MessageBuilder.withPayload(payment).build())));

        verify(handler).handleEvent(payment);
        verifyNoMoreInteractions(handler);
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
    }
}
