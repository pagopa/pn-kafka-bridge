package it.pagopa.pn.kafka.bridge.middleware.kafka.mapper;

import it.pagopa.pn.api.dto.events.PnPtDelegationPayload;
import it.pagopa.pn.kafka.bridge.mapper.DelegationMapper;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DelegationMapperTest {

    private final DelegationMapper mapper = new DelegationMapper();

    @Test
    void toPnPtDelegationPayloadCompleteMessageAllFieldsMappedTest() {
        DelegationSelfCareMessage message = completeMessage();

        PnPtDelegationPayload actual = mapper.toPnPtDelegationPayload(message);

        assertThat(actual.getDelegationId()).isEqualTo("delegation-1");
        assertThat(actual.getDelegationType()).isEqualTo("PT");
        assertThat(actual.getProductId()).isEqualTo("prod-pn-dev");
        assertThat(actual.getStatus()).isEqualTo("DELETED");
        assertThat(actual.getIsTest()).isTrue();
        assertThat(actual.getSourceEventId()).isEqualTo("event-1");
        assertThat(actual.getSourceEventType()).isEqualTo("UPDATE");
        assertThat(actual.getCreatedAt()).isEqualTo(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        assertThat(actual.getUpdatedAt()).isEqualTo(Instant.parse("2024-08-10T09:15:00.123Z"));
        assertThat(actual.getClosedAt()).isEqualTo(Instant.parse("2024-09-11T07:05:30Z"));
        assertThat(actual.getInstitutionId()).isEqualTo("from-1");
        assertThat(actual.getInstitutionName()).isEqualTo("Comune di Milano");
        assertThat(actual.getInstitutionRootName()).isEqualTo("Comune di Milano Root");
        assertThat(actual.getInstitutionTaxCode()).isEqualTo("00488410010");
        assertThat(actual.getInstitutionType()).isEqualTo("PA");
        assertThat(actual.getPtId()).isEqualTo("to-1");
        assertThat(actual.getPtName()).isEqualTo("Partner Tecnologico Srl");
        assertThat(actual.getPtTaxCode()).isEqualTo("01234567890");
        assertThat(actual.getPtType()).isEqualTo("PT");
    }

    @Test
    void toPnPtDelegationPayloadMessageWithoutOptionalFieldsOptionalFieldsNullTest() {
        DelegationSelfCareMessage message = completeMessage();
        message.setIsTest(null);
        message.setEventType(null);
        message.setUpdatedAt(null);
        message.setClosedAt(null);
        message.setInstitutionFromRootName(null);

        PnPtDelegationPayload actual = mapper.toPnPtDelegationPayload(message);

        assertThat(actual.getIsTest()).isNull();
        assertThat(actual.getSourceEventType()).isNull();
        assertThat(actual.getUpdatedAt()).isNull();
        assertThat(actual.getClosedAt()).isNull();
        assertThat(actual.getInstitutionRootName()).isNull();
        assertThat(actual.getDelegationId()).isEqualTo("delegation-1");
        assertThat(actual.getSourceEventId()).isEqualTo("event-1");
        assertThat(actual.getCreatedAt()).isEqualTo(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        assertThat(actual.getPtTaxCode()).isEqualTo("01234567890");
    }

    private DelegationSelfCareMessage completeMessage() {
        DelegationSelfCareMessage message = new DelegationSelfCareMessage();
        message.setId("event-1");
        message.setDelegationId("delegation-1");
        message.setFrom("from-1");
        message.setInstitutionFromName("Comune di Milano");
        message.setInstitutionFromRootName("Comune di Milano Root");
        message.setInstitutionToName("Partner Tecnologico Srl");
        message.setTo("to-1");
        message.setToTaxCode("01234567890");
        message.setFromTaxCode("00488410010");
        message.setToType("PT");
        message.setFromType("PA");
        message.setProductId("prod-pn-dev");
        message.setType("PT");
        message.setStatus("DELETED");
        message.setCreatedAt(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        message.setUpdatedAt(Instant.parse("2024-08-10T09:15:00.123Z"));
        message.setClosedAt(Instant.parse("2024-09-11T07:05:30Z"));
        message.setIsTest(true);
        message.setEventType("UPDATE");
        return message;
    }
}
