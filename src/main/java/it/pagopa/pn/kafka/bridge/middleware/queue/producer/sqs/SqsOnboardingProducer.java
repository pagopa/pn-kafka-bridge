package it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.PnOnboardingInstitutionEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsOnboardingProducer extends AbstractSqsMomProducer<PnOnboardingInstitutionEvent> {

    public SqsOnboardingProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnOnboardingInstitutionEvent.class );
    }
}
