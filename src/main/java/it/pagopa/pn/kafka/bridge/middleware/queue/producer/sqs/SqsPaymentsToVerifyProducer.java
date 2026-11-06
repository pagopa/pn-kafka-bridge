package it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsPaymentsToVerifyProducer extends AbstractSqsMomProducer<PnNoticePaymentEvent> {

    public SqsPaymentsToVerifyProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnNoticePaymentEvent.class );
    }
}
