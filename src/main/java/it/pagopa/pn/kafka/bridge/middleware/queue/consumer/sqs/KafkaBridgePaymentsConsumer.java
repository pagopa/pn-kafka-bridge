package it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs.handler.PagoPaPaymentMessageHandler;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.util.MdcKey;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.kafka.bridge.util.MdcUtils.setMdc;


@Configuration
@CustomLog
@RequiredArgsConstructor
public class KafkaBridgePaymentsConsumer {

    private final PagoPaPaymentMessageHandler pagoPaPaymentMessageHandler;

    @SqsListener(value = "${pn.kafka-bridge.sqs-topics.kafka-bridge-payments}")
    public void consume(Message<PagoPaPaymentMessage> message) {
        final String processName = "PAYMENTS MESSAGE";
        setMdc(message);
        log.debug("Handle PagoPaPaymentMessage, with content {}", message);
        try {
            MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.PAYMENTS_KEY);
            log.logStartingProcess(processName);
            pagoPaPaymentMessageHandler.handleEvent(message.getPayload());
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
        }
    }
}