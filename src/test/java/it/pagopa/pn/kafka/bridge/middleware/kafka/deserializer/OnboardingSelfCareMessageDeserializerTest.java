package it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer;

import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OnboardingSelfCareMessageDeserializerTest {

    @RegisterExtension
    private final ExpectedLogging logging = ExpectedLogging.forSource(OnboardingSelfCareMessageDeserializer.class);


    private OnboardingSelfCareMessageDeserializer deserializer;

    @BeforeEach
    public void init() {
        deserializer = new OnboardingSelfCareMessageDeserializer();
    }


    @Test
    void deserializeTest() {
        String requestFromSelfCare = inputRequestFormSelfCare();

        OnboardingSelfCareMessage actual = deserializer.deserialize("topic", requestFromSelfCare.getBytes());

        assertThat(actual).isNotNull();
        assertThat(actual.getInternalIstitutionID()).isEqualTo("7861b02d-8cb4-4de9-95d2-5ed02f3de38a");
        assertThat(actual.getState()).isEqualTo("ACTIVE");
        assertThat(actual.getUpdatedAt()).isEqualTo(Instant.parse("2023-01-10T15:20:38.94Z"));
        assertThat(actual.getInstitution().getAddress()).isEqualTo("Piazza Umberto I, 1");
        assertThat(actual.getInstitution().getDigitalAddress()).isEqualTo("protocollo@comunetovosangiacomo.it");
        assertThat(actual.getInstitution().getTaxCode()).isEqualTo("00338460090");
        assertThat(actual.getInstitution().getDescription()).isEqualTo("Comune di Tovo San Giacomo");
    }

    @Test
    void deserializeWithNullTest() {
        OnboardingSelfCareMessage actual = deserializer.deserialize("topic", null);
        assertThat(actual).isNull();

    }

    @Test
    void deserializeInvalidJsonTest() {
        byte[] bytes = "prova".getBytes();
        OnboardingSelfCareMessage actual = deserializer.deserialize("topic", bytes);
        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging).hasWarningMessageMatching(Pattern.compile("Error when deserializing.*").pattern());
    }

    @Test
    void deserializeWithAdditionFieldForNonPNProductTest() {
        byte[] bytes = inputRequestWithAdditionalFieldNonPN().getBytes();
        OnboardingSelfCareMessage actual = deserializer.deserialize("topic", bytes);
        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging).hasWarningMessage("Error when deserializing byte[] to OnboardingSelfCareMessage with input: " + new String(bytes));
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @Test
    void deserializeWithAdditionFieldForPNProductTest() {
        byte[] bytes = inputRequestWithAdditionalFieldPN().getBytes();
        OnboardingSelfCareMessage actual = deserializer.deserialize("topic", bytes);
        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessage("Error when deserializing byte[] to OnboardingSelfCareMessage with input: " + new String(bytes));
        ExpectedLoggingAssertions.assertThat(logging).hasNoWarningMessage();
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

    private String inputRequestWithAdditionalFieldNonPN() {
        return """
                {
                  "billing":{
                    "recipientCode":"new ",
                    "vatNumber":"00121930789"
                  },
                  "contentType":"application/octet-stream",
                  "fileName":"App IO_accordo_adesione (25).pdf4818769499989222848.pdf",
                  "filePath":"parties/docs/7cefb0b9-9296-4025-a7a4-0874cd732f32/App IO_accordo_adesione (25).pdf4818769499989222848.pdf",
                  "id":"7cefb0b9-9296-4025-a7a4-0874cd732f32",
                  "institution":{
                    "address":"Via rossi,194",
                    "description":"prova onboarding ",
                    "digitalAddress":"pectest@pec.test.it",
                    "institutionType":"GSP",
                    "origin":"SELC",
                    "originId":"GSP_00121930789",
                    "taxCode":"00121930789"
                  },
                  "internalIstitutionID":"10189036-35fe-4c03-bc27-d8d14d22a02e",
                  "onboardingTokenId":"7cefb0b9-9296-4025-a7a4-0874cd732f32",
                  "pricingPlan":"C1",
                  "product":"prod-io-premium",
                  "state":"ACTIVE",
                  "updatedAt":"2023-02-02T14:13:28.816Z",
                  "additionField: "additionValue"
                }
                """;
    }

    private String inputRequestWithAdditionalFieldPN() {
        return """
                {
                  "billing":{
                    "recipientCode":"new ",
                    "vatNumber":"00121930789"
                  },
                  "contentType":"application/octet-stream",
                  "fileName":"App IO_accordo_adesione (25).pdf4818769499989222848.pdf",
                  "filePath":"parties/docs/7cefb0b9-9296-4025-a7a4-0874cd732f32/App IO_accordo_adesione (25).pdf4818769499989222848.pdf",
                  "id":"7cefb0b9-9296-4025-a7a4-0874cd732f32",
                  "institution":{
                    "address":"Via rossi,194",
                    "description":"prova onboarding ",
                    "digitalAddress":"pectest@pec.test.it",
                    "institutionType":"GSP",
                    "origin":"SELC",
                    "originId":"GSP_00121930789",
                    "taxCode":"00121930789"
                  },
                  "internalIstitutionID":"10189036-35fe-4c03-bc27-d8d14d22a02e",
                  "onboardingTokenId":"7cefb0b9-9296-4025-a7a4-0874cd732f32",
                  "pricingPlan":"C1",
                  "product":"prod-pn-dev",
                  "state":"ACTIVE",
                  "updatedAt":"2023-02-02T14:13:28.816Z",
                  "additionField: "additionValue"
                }
                """;
    }

}
