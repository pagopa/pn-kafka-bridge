package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MdcUtilsTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void copiesMessageIdTraceIdAndIunFromHeaders() {
        var message = MessageBuilder.withPayload("payment")
                .setHeader("aws_messageId", "message-id")
                .setHeader("X-Amzn-Trace-Id", "trace-id")
                .setHeader("iun", "notification-iun")
                .build();

        MdcUtils.setMdc(message);

        assertEquals("message-id", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertEquals("trace-id", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        assertEquals("notification-iun", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
    }

    @Test
    void generatesUuidTraceIdWhenTraceHeaderIsMissing() {
        MdcUtils.setMdc(MessageBuilder.withPayload("payment").build());

        String traceId = MDC.get(MDCUtils.MDC_TRACE_ID_KEY);
        assertNotNull(traceId);
        assertEquals(traceId, UUID.fromString(traceId).toString());
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertNull(MDC.get(MDCUtils.MDC_PN_IUN_KEY));
    }

    @Test
    void clearsPreviousMessageContextAndGeneratesANewTraceForEachMessage() {
        MdcUtils.setMdc(MessageBuilder.withPayload("first")
                .setHeader("aws_messageId", "previous-message")
                .setHeader("iun", "previous-iun")
                .build());
        String previousTrace = MDC.get(MDCUtils.MDC_TRACE_ID_KEY);
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.PAYMENTS_KEY);

        MdcUtils.setMdc(MessageBuilder.withPayload("second").build());

        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertNull(MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_TOPIC));
        String traceId = MDC.get(MDCUtils.MDC_TRACE_ID_KEY);
        assertNotNull(traceId);
        assertEquals(traceId, UUID.fromString(traceId).toString());
        assertNotEquals(previousTrace, traceId);
    }
}
