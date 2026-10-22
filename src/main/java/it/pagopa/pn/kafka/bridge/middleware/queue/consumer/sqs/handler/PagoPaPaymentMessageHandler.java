package it.pagopa.pn.kafka.bridge.middleware.queue.consumer.sqs.handler;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoPaPaymentMessageHandler {

    public static final String ERROR_CODE_PAYMENT_EVENTFAILED = "PN_PAYMENT_EVENTFAILED";
    private final MomProducer<PnNoticePaymentEvent> paymentsToVerifyProducer;
    private final PagoPaPaymentMapper mapper;

    public void handleEvent(PagoPaPaymentMessage evt ) {
        try {
            //TODO è corretto mettere Instant.now?
            PnNoticePaymentEvent event = mapper.toPnNoticePaymentEvent(evt, Instant.now());
            paymentsToVerifyProducer.push(event);
        } catch (Exception e) {
            log.error("error handling event", e);
            throw new PnInternalException("Error handling payment message", ERROR_CODE_PAYMENT_EVENTFAILED, e);
        }

    }

}
