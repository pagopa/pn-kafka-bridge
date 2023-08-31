package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.OnboardingService;
import it.pagopa.pn.kafka.bridge.util.ValidatorUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.concurrent.ExecutionException;


@SpringBootTest(properties = {
        "spring.kafka.consumer.bootstrap-servers=PLAINTEXT://localhost:9092",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "pn.kafka-bridge.onboarding-group-id=consumer-test",
        "spring.kafka.consumer.properties.security.protocol=PLAINTEXT",
        "pn.kafka-bridge.selfcare-pn-product-id=prod-pn-dev"
})
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class OnboardingSelfCareConsumerTestIT {

    @RegisterExtension
    private final ExpectedLogging logging = ExpectedLogging.forSource(ValidatorUtil.class);

    @SpyBean
    private OnboardingSelfCareConsumer consumer;

    @SpyBean
    private OnboardingService onboardingService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SqsOnboardingProducer sqsOnboardingProducer;


    @Test
    void listenOKTest() throws ExecutionException, InterruptedException, JsonProcessingException {

        String inputRequest = inputRequestFormSelfCare();

        //scrivo su Kafka una Request presa dal flusso reale di SelfCare
        kafkaTemplate.send("sc-contracts", inputRequest).get();

        OnboardingSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, OnboardingSelfCareMessage.class);

        //verifico che il consumer riceva correttamente il messaggio (e quindi il deserializer funzioni)
        Mockito.verify(consumer, Mockito.timeout(1000).times(1)).listen("sc-contracts", expectedValue);
        Mockito.verify(onboardingService, Mockito.timeout(1000).times(1)).sendMessage(expectedValue);
        awaitFlow();
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();

    }

    @Test
    void listenNotReceivedMessageForFiltering() throws ExecutionException, InterruptedException, JsonProcessingException {

        String inputRequest = inputRequestFormSelfCareWithNotPNProduct();

        //scrivo su Kafka una Request presa dal flusso reale di SelfCare
        kafkaTemplate.send("sc-contracts", inputRequest).get();

        OnboardingSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, OnboardingSelfCareMessage.class);

        //verifico che il messaggio non venga ricevuto dal consumer perché scartato dal filter
        Thread.sleep(2000L);
        Mockito.verify(consumer, Mockito.never()).listen("sc-contracts", expectedValue);

    }

    @Test
    void listenMessageWithInvalidTaxIdTest() throws ExecutionException, InterruptedException, JsonProcessingException {

        String inputRequest = inputRequestFormSelfCare();
        inputRequest = inputRequest.replace("00338460090", "00338460090,");

        //scrivo su Kafka una Request presa dal flusso reale di SelfCare
        kafkaTemplate.send("sc-contracts", inputRequest).get();

        OnboardingSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, OnboardingSelfCareMessage.class);

        //verifico che il consumer riceva correttamente il messaggio (e quindi il deserializer funzioni)
        Mockito.verify(consumer, Mockito.timeout(1000).times(1)).listen("sc-contracts", expectedValue);

        //verifico che il messaggio non arrivi al service perché non supera la validazione
        Mockito.verify(onboardingService, Mockito.timeout(1000).times(0)).sendMessage(expectedValue);
        awaitFlow();
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessageMatching(".*Payload received is not valid.*billing.vatNumber.*");

    }

    @Test
    void listenMessageWithNullTaxIdTest() throws ExecutionException, InterruptedException, JsonProcessingException {

        String inputRequest = inputRequestFormSelfCare();
        inputRequest = inputRequest.replace("\"00338460090\"", "null");

        //scrivo su Kafka una Request presa dal flusso reale di SelfCare
        kafkaTemplate.send("sc-contracts", inputRequest).get();

        OnboardingSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, OnboardingSelfCareMessage.class);

        //verifico che il consumer riceva correttamente il messaggio (e quindi il deserializer funzioni)
        Mockito.verify(consumer, Mockito.timeout(1000).times(1)).listen("sc-contracts", expectedValue);

        //verifico che il messaggio non arrivi al service perché non supera la validazione
        Mockito.verify(onboardingService, Mockito.timeout(1000).times(0)).sendMessage(expectedValue);
        awaitFlow();
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessageMatching(".*Payload received is not valid.*billing.vatNumber.*");
    }

    private String inputRequestFormSelfCare() {
        return """
                {
                   "billing":{
                      "recipientCode":"bc_0432",
                      "vatNumber":"00338460090"
                   },
                   "contentType":"application/octet-stream",
                   "fileName":"App IO_accordo_adesione.pdf7419256794741715935.pdf",
                   "filePath":"parties/docs/7014954b-5a2f-4aed-9f26-b2b778c2a120/App IO_accordo_adesione.pdf7419256794741715935.pdf",
                   "id":"7014954b-5a2f-4aed-9f26-b2b778c2a120",
                   "institution":{
                      "address":"Piazza Umberto I, 1",
                      "description":"Comune di Tovo San Giacomo",
                      "digitalAddress":"protocollo@comunetovosangiacomo.it",
                      "institutionType":"PA",
                      "origin":"IPA",
                      "originId":"c_l315",
                      "taxCode":"00338460090"
                   },
                   "internalIstitutionID":"7861b02d-8cb4-4de9-95d2-5ed02f3de38a",
                   "onboardingTokenId":"7014954b-5a2f-4aed-9f26-b2b778c2a120",
                   "product":"prod-pn-dev",
                   "state":"ACTIVE",
                   "updatedAt":"2023-01-10T15:20:38.94Z",
                   "createdAt":"2023-01-05T13:41:30.621Z",
                   "zipCode":"02045"
                }
                """;
    }

    private String inputRequestFormSelfCareWithNotPNProduct() {
        return """
                {
                   "billing":{
                      "recipientCode":"bc_0432",
                      "vatNumber":"00338460090"
                   },
                   "contentType":"application/octet-stream",
                   "fileName":"App IO_accordo_adesione.pdf7419256794741715935.pdf",
                   "filePath":"parties/docs/7014954b-5a2f-4aed-9f26-b2b778c2a120/App IO_accordo_adesione.pdf7419256794741715935.pdf",
                   "id":"7014954b-5a2f-4aed-9f26-b2b778c2a120",
                   "institution":{
                      "address":"Piazza Umberto I, 1",
                      "description":"Comune di Tovo San Giacomo",
                      "digitalAddress":"protocollo@comunetovosangiacomo.it",
                      "institutionType":"PA",
                      "origin":"IPA",
                      "originId":"c_l315",
                      "taxCode":"00338460090"
                   },
                   "internalIstitutionID":"7861b02d-8cb4-4de9-95d2-5ed02f3de38a",
                   "onboardingTokenId":"7014954b-5a2f-4aed-9f26-b2b778c2a120",
                   "product":"prod-io-dev",
                   "state":"ACTIVE",
                   "updatedAt":"2023-01-10T15:20:38.94Z",
                   "createdAt":"2023-01-05T13:41:30.621Z",
                   "zipCode":"02045"
                }
                """;
    }

    private void awaitFlow() {
        try {
            Thread.sleep(1000L);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
