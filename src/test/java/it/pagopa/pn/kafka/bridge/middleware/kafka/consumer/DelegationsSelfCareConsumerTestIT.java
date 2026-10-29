package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import it.pagopa.pn.api.dto.events.PnPtDelegationEvent;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsDelegationsProducer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.DelegationService;
import it.pagopa.pn.kafka.bridge.util.ValidatorUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.properties.security.protocol=PLAINTEXT",
        "pn.kafka-bridge.onboarding-group-id=delegations-it-onboarding",
        "pn.kafka-bridge.delegations.enabled=true",
        "pn.kafka-bridge.delegations.topic-name=sc-delegations",
        "pn.kafka-bridge.delegations.group-id=delegations-it-consumer",
        "pn.kafka-bridge.delegations.queue-name=local-delegations",
        "pn.kafka-bridge.delegations.sasl-jaas-password=unused",
        "pn.kafka-bridge.selfcare-pn-product-id=prod-pn-dev"
})
@EmbeddedKafka(partitions = 1)
class DelegationsSelfCareConsumerTestIT {

    @RegisterExtension
    private final ExpectedLogging logging = ExpectedLogging.forSource(ValidatorUtil.class);

    @MockitoSpyBean
    private DelegationsSelfCareConsumer consumer;

    @MockitoSpyBean
    private DelegationService delegationService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SqsDelegationsProducer sqsDelegationsProducer;

    @MockitoBean
    private SqsOnboardingProducer sqsOnboardingProducer;

    @Test
    void listenPtDelegationOfSendProductPublishedTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String inputRequest = delegationEvent("PT", "prod-pn-dev");

        kafkaTemplate.send("sc-delegations", inputRequest).get();

        DelegationSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, DelegationSelfCareMessage.class);
        Mockito.verify(consumer, Mockito.timeout(10000).times(1)).listen("sc-delegations", expectedValue);
        Mockito.verify(delegationService, Mockito.timeout(10000).times(1)).sendMessage(expectedValue);
        ArgumentCaptor<PnPtDelegationEvent> captor = ArgumentCaptor.forClass(PnPtDelegationEvent.class);
        Mockito.verify(sqsDelegationsProducer, Mockito.timeout(10000).times(1)).push(captor.capture());
        assertThat(captor.getValue().getHeader().getIun()).isEqualTo("6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22");
        assertThat(captor.getValue().getPayload().getDelegationType()).isEqualTo("PT");
        Mockito.verify(sqsDelegationsProducer, Mockito.after(1000).times(1)).push(Mockito.any(PnPtDelegationEvent.class));
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @Test
    void listenNotPtDelegationIgnoredTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String inputRequest = delegationEvent("AOO", "prod-pn-dev");

        kafkaTemplate.send("sc-delegations", inputRequest).get();

        DelegationSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, DelegationSelfCareMessage.class);
        Mockito.verify(consumer, Mockito.after(3000).never()).listen("sc-delegations", expectedValue);
        Mockito.verify(delegationService, Mockito.never()).sendMessage(expectedValue);
    }

    @Test
    void listenPtDelegationOfOtherProductIgnoredTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String inputRequest = delegationEvent("PT", "prod-other");

        kafkaTemplate.send("sc-delegations", inputRequest).get();

        DelegationSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, DelegationSelfCareMessage.class);
        Mockito.verify(consumer, Mockito.after(3000).never()).listen("sc-delegations", expectedValue);
        Mockito.verify(delegationService, Mockito.never()).sendMessage(expectedValue);
        Mockito.verify(sqsDelegationsProducer, Mockito.never()).push(Mockito.any(PnPtDelegationEvent.class));
    }

    @Test
    void listenDelegationWithoutOptionalFieldsAcceptedTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String inputRequest = delegationEventWithoutOptionalFields();

        kafkaTemplate.send("sc-delegations", inputRequest).get();

        DelegationSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, DelegationSelfCareMessage.class);
        Mockito.verify(consumer, Mockito.timeout(10000).times(1)).listen("sc-delegations", expectedValue);
        Mockito.verify(delegationService, Mockito.timeout(10000).times(1)).sendMessage(expectedValue);
        Mockito.verify(sqsDelegationsProducer, Mockito.after(1000).times(1)).push(Mockito.any(PnPtDelegationEvent.class));
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @Test
    void listenDelegationWithMissingMandatoryFieldNotPublishedTest() throws ExecutionException, InterruptedException, JsonProcessingException {
        String inputRequest = delegationEvent("PT", "prod-pn-dev")
                .replace("\"toTaxCode\":\"01234567890\",", "");

        kafkaTemplate.send("sc-delegations", inputRequest).get();

        DelegationSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, DelegationSelfCareMessage.class);
        Mockito.verify(consumer, Mockito.timeout(10000).times(1)).listen("sc-delegations", expectedValue);
        Mockito.verify(delegationService, Mockito.after(2000).never()).sendMessage(expectedValue);
        Mockito.verify(sqsDelegationsProducer, Mockito.never()).push(Mockito.any(PnPtDelegationEvent.class));
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessageMatching(".*Payload received is not valid.*toTaxCode.*");
    }

    private String delegationEvent(String type, String productId) {
        return """
                {
                   "id":"0f3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a11",
                   "delegationId":"6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22",
                   "from":"b1f3c2d4-0a1b-4c2d-8e3f-4a5b6c7d8e9f",
                   "institutionFromName":"Comune di Milano",
                   "institutionToName":"Partner Tecnologico Srl",
                   "institutionFromRootName":"Comune di Milano Root",
                   "to":"c2a4d3e5-1b2c-4d3e-9f40-5b6c7d8e9f01",
                   "toTaxCode":"01234567890",
                   "fromTaxCode":"00488410010",
                   "toType":"PT",
                   "fromType":"PA",
                   "productId":"%s",
                   "type":"%s",
                   "status":"ACTIVE",
                   "createdAt":"2024-07-09T12:40:21.414946089Z",
                   "updatedAt":"2024-08-10T09:15:00.123Z",
                   "isTest":true,
                   "eventType":"ADD"
                }
                """.formatted(productId, type);
    }

    private String delegationEventWithoutOptionalFields() {
        return """
                {
                   "id":"1a3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a12",
                   "delegationId":"7a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a23",
                   "from":"b1f3c2d4-0a1b-4c2d-8e3f-4a5b6c7d8e9f",
                   "institutionFromName":"Comune di Milano",
                   "institutionToName":"Partner Tecnologico Srl",
                   "to":"c2a4d3e5-1b2c-4d3e-9f40-5b6c7d8e9f01",
                   "toTaxCode":"01234567890",
                   "fromTaxCode":"00488410010",
                   "toType":"PT",
                   "fromType":"PA",
                   "productId":"prod-pn-dev",
                   "type":"PT",
                   "status":"ACTIVE",
                   "createdAt":"2024-07-09T12:40:21.414946089Z"
                }
                """;
    }
}
