package it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer;

import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DelegationSelfCareMessageDeserializerTest {

    @RegisterExtension
    private final ExpectedLogging logging = ExpectedLogging.forSource(DelegationSelfCareMessageDeserializer.class);

    private DelegationSelfCareMessageDeserializer deserializer;

    @BeforeEach
    void init() {
        deserializer = new DelegationSelfCareMessageDeserializer();
    }

    @Test
    void deserializeCompleteEventAllFieldsMappedTest() {
        DelegationSelfCareMessage actual = deserializer.deserialize("topic", completeEvent("prod-pn-dev", "true").getBytes());

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo("0f3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a11");
        assertThat(actual.getDelegationId()).isEqualTo("6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22");
        assertThat(actual.getFrom()).isEqualTo("b1f3c2d4-0a1b-4c2d-8e3f-4a5b6c7d8e9f");
        assertThat(actual.getInstitutionFromName()).isEqualTo("Comune di Milano");
        assertThat(actual.getInstitutionToName()).isEqualTo("Partner Tecnologico Srl");
        assertThat(actual.getInstitutionFromRootName()).isEqualTo("Comune di Milano Root");
        assertThat(actual.getTo()).isEqualTo("c2a4d3e5-1b2c-4d3e-9f40-5b6c7d8e9f01");
        assertThat(actual.getToTaxCode()).isEqualTo("01234567890");
        assertThat(actual.getFromTaxCode()).isEqualTo("00488410010");
        assertThat(actual.getToType()).isEqualTo("PT");
        assertThat(actual.getFromType()).isEqualTo("PA");
        assertThat(actual.getProductId()).isEqualTo("prod-pn-dev");
        assertThat(actual.getType()).isEqualTo("PT");
        assertThat(actual.getStatus()).isEqualTo("DELETED");
        assertThat(actual.getCreatedAt()).isEqualTo(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        assertThat(actual.getUpdatedAt()).isEqualTo(Instant.parse("2024-08-10T09:15:00.123Z"));
        assertThat(actual.getClosedAt()).isEqualTo(Instant.parse("2024-09-11T07:05:30Z"));
        assertThat(actual.getIsTest()).isTrue();
        assertThat(actual.getEventType()).isEqualTo("UPDATE");
    }

    @Test
    void deserializeIsTestFalseMappedAsFalseTest() {
        DelegationSelfCareMessage actual = deserializer.deserialize("topic", completeEvent("prod-pn-dev", "false").getBytes());

        assertThat(actual).isNotNull();
        assertThat(actual.getIsTest()).isFalse();
    }

    @Test
    void deserializeEventWithoutOptionalFieldsOptionalFieldsNullTest() {
        DelegationSelfCareMessage actual = deserializer.deserialize("topic", eventWithoutOptionalFields().getBytes());

        assertThat(actual).isNotNull();
        assertThat(actual.getDelegationId()).isEqualTo("6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22");
        assertThat(actual.getType()).isEqualTo("PT");
        assertThat(actual.getStatus()).isEqualTo("ACTIVE");
        assertThat(actual.getCreatedAt()).isEqualTo(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        assertThat(actual.getIsTest()).isNull();
        assertThat(actual.getEventType()).isNull();
        assertThat(actual.getUpdatedAt()).isNull();
        assertThat(actual.getClosedAt()).isNull();
        assertThat(actual.getInstitutionFromRootName()).isNull();
    }

    @Test
    void deserializeUnknownPropertyIgnoredTest() {
        String json = completeEvent("prod-pn-dev", "true").replace("\"eventType\"", "\"unknownField\":\"x\",\"eventType\"");

        DelegationSelfCareMessage actual = deserializer.deserialize("topic", json.getBytes());

        assertThat(actual).isNotNull();
        assertThat(actual.getDelegationId()).isEqualTo("6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22");
    }

    @Test
    void deserializeNullDataReturnsNullTest() {
        DelegationSelfCareMessage actual = deserializer.deserialize("topic", null);

        assertThat(actual).isNull();
    }

    @Test
    void deserializeInvalidJsonReturnsNullWithoutErrorLogTest() {
        DelegationSelfCareMessage actual = deserializer.deserialize("topic", "prova".getBytes());

        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @Test
    void deserializeInvalidTimestampForPnProductReturnsNullWithErrorLogTest() {
        byte[] bytes = completeEvent("prod-pn-dev", "true").replace("2024-08-10T09:15:00.123Z", "not-a-date").getBytes();

        DelegationSelfCareMessage actual = deserializer.deserialize("topic", bytes);

        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging)
                .hasErrorMessage("Error when deserializing byte[] to DelegationSelfCareMessage with input: " + new String(bytes));
    }

    @Test
    void deserializeInvalidTimestampForOtherProductReturnsNullWithoutErrorLogTest() {
        byte[] bytes = completeEvent("prod-io", "true").replace("2024-08-10T09:15:00.123Z", "not-a-date").getBytes();

        DelegationSelfCareMessage actual = deserializer.deserialize("topic", bytes);

        assertThat(actual).isNull();
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    private String completeEvent(String productId, String isTest) {
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
                   "type":"PT",
                   "status":"DELETED",
                   "createdAt":"2024-07-09T12:40:21.414946089Z",
                   "updatedAt":"2024-08-10T09:15:00.123Z",
                   "closedAt":"2024-09-11T07:05:30Z",
                   "isTest":%s,
                   "eventType":"UPDATE"
                }
                """.formatted(productId, isTest);
    }

    private String eventWithoutOptionalFields() {
        return """
                {
                   "id":"0f3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a11",
                   "delegationId":"6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22",
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
