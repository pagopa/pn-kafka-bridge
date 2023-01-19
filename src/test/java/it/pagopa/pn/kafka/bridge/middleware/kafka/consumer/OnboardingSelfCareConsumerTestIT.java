package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.LocalStackTestConfig;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.ExecutionException;


@SpringBootTest(properties = {
        "spring.kafka.consumer.bootstrap-servers=PLAINTEXT://localhost:9092",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "pn.kafka-bridge.onboarding-group-id=consumer-test",
        "spring.kafka.consumer.properties.security.protocol=PLAINTEXT"
})
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@Import(LocalStackTestConfig.class)
class OnboardingSelfCareConsumerTestIT {

    @SpyBean
    private OnboardingSelfCareConsumer consumer;

    @SpyBean
    private OnboardingService onboardingService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void listenOKTest() throws ExecutionException, InterruptedException, JsonProcessingException {

        String inputRequest = inputRequestFormSelfCare();

        //scrivo su Kafka una Request presa dal flusso reale di SelfCare
        kafkaTemplate.send("sc-contracts", inputRequest).get();

        OnboardingSelfCareMessage expectedValue = objectMapper.readValue(inputRequest, OnboardingSelfCareMessage.class);

        //verifico che il consumer riceva correttamente il messaggio (e quindi il deserializer funzioni)
        Mockito.verify(consumer, Mockito.timeout(1000).times(1)).listen("sc-contracts", expectedValue);
        Mockito.verify(onboardingService, Mockito.timeout(1000).times(1)).sendMessage(expectedValue);

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
                   "product":"prod-io",
                   "state":"ACTIVE",
                   "updatedAt":"2023-01-10T15:20:38.94Z"
                }
                """;
    }

}
